package com.edwardjones.demo.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST + SSE endpoints for HNW financial alerts sourced from Databricks.
 *
 * GET  /api/alerts/stream?clientId=xxx  — SSE stream (React EventSource)
 * PATCH /api/alerts/{alertId}/read      — mark alert as read in Databricks
 * GET  /api/alerts/health               — Databricks connectivity check
 * POST /api/alerts/test?scenario=X      — dev-only: broadcast a mock alert without Databricks
 *                                          scenarios: concentration (default), harvest, drift, compliance
 */
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")  // tighten for production; fine for demo
public class AlertController {

    private static final Logger log = LoggerFactory.getLogger(AlertController.class);

    private final SseEmitterRegistry sseRegistry;
    private final AlertPollingService pollingService;
    private final DataSource databricksDataSource;

    @Value("${databricks.catalog:workspace}")
    private String catalog;

    @Value("${databricks.schema:default}")
    private String schema;

    public AlertController(
            SseEmitterRegistry sseRegistry,
            AlertPollingService pollingService,
            @Qualifier("databricksDataSource") DataSource databricksDataSource) {
        this.sseRegistry = sseRegistry;
        this.pollingService = pollingService;
        this.databricksDataSource = databricksDataSource;
    }

    /**
     * SSE stream endpoint.
     * React AlertTray connects here on mount:
     *   new EventSource('/api/alerts/stream?clientId=' + crypto.randomUUID())
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String clientId) {
        log.info("New SSE connection requested for clientId: {}", clientId);
        return sseRegistry.register(clientId);
    }

    /**
     * Mark an alert as read in the Databricks Delta table.
     * Called when the advisor clicks Dismiss on an AlertCard.
     */
    @PatchMapping("/{alertId}/read")
    public ResponseEntity<Void> markRead(@PathVariable String alertId) {
        String sql = String.format(
            "UPDATE %s.%s.hnw_alerts SET is_read = true WHERE alert_id = ?",
            catalog, schema
        );

        try (Connection conn = databricksDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alertId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                log.warn("markRead: no alert found with id {}", alertId);
                return ResponseEntity.notFound().build();
            }
            log.info("Alert {} marked as read", alertId);
            return ResponseEntity.noContent().build();
        } catch (SQLException e) {
            log.error("Failed to mark alert {} as read: {}", alertId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Quick connectivity health check — verifies Databricks JDBC is reachable.
     * Useful for smoke-testing after deployment.
     * GET /api/alerts/health
     */
    @GetMapping("/health")
    public ResponseEntity<DatabricksHealth> health() {
        boolean connected = pollingService.testConnection();
        int clients = sseRegistry.connectedClientCount();
        return ResponseEntity.ok(new DatabricksHealth(connected, clients));
    }

    public record DatabricksHealth(boolean databricksConnected, int sseClientsConnected) {}

    // ── Demo / dev-only ───────────────────────────────────────────────────────

    private static final String DEMO_CLIENT_ID = "00000000-0000-0000-0000-000000000001";

    private static final Map<String, HnwAlert> DEMO_SCENARIOS = Map.of(
        "concentration", new HnwAlert(
            UUID.randomUUID().toString(),
            DEMO_CLIENT_ID,
            "CONCENTRATION",
            "HIGH",
            "LIVE: TSLA concentration spike detected",
            "AAPL, MSFT, AMZN, and TSLA combined represent 45% of the taxable portfolio. " +
            "Sector concentration above 40% warrants a discussion about diversification strategy.",
            "TSLA",
            Instant.now(),
            false
        ),
        "harvest", new HnwAlert(
            UUID.randomUUID().toString(),
            DEMO_CLIENT_ID,
            "HARVEST_OPPORTUNITY",
            "MEDIUM",
            "Tax-loss harvest window open: INTC lot #2",
            "INTC Lot #2 (purchased 2023-03-14) has an unrealized loss of $4,820. " +
            "Harvesting now could offset $1,157 in federal taxes before year-end.",
            "INTC",
            Instant.now(),
            false
        ),
        "drift", new HnwAlert(
            UUID.randomUUID().toString(),
            DEMO_CLIENT_ID,
            "DRIFT",
            "MEDIUM",
            "Allocation drift: Fixed Income under-weight",
            "Fixed Income has drifted to 14.2% vs. a 20% target (−5.8 pts, −29% relative). " +
            "This exceeds the 5/25 rebalancing threshold.",
            null,
            Instant.now(),
            false
        ),
        "compliance", new HnwAlert(
            UUID.randomUUID().toString(),
            DEMO_CLIENT_ID,
            "COMPLIANCE",
            "HIGH",
            "Wash-sale risk: TSLA repurchase within 30 days",
            "A TSLA lot was sold on 2024-12-05 to harvest a loss. A new TSLA position was " +
            "opened on 2024-12-18 — within the 30-day wash-sale window. The harvested loss " +
            "may be disallowed.",
            "TSLA",
            Instant.now(),
            false
        )
    );

    /**
     * Dev-only endpoint — broadcasts a synthetic alert to all connected SSE clients.
     * Does NOT touch Databricks. Useful when Databricks is unavailable during a demo.
     *
     * POST /api/alerts/test?scenario=concentration   (default)
     * POST /api/alerts/test?scenario=harvest
     * POST /api/alerts/test?scenario=drift
     * POST /api/alerts/test?scenario=compliance
     * POST /api/alerts/test?scenario=all             — fires all four scenarios
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> broadcastTestAlert(
            @RequestParam(defaultValue = "concentration") String scenario) {

        List<HnwAlert> toSend;

        if ("all".equalsIgnoreCase(scenario)) {
            toSend = List.copyOf(DEMO_SCENARIOS.values());
        } else {
            HnwAlert alert = DEMO_SCENARIOS.get(scenario.toLowerCase());
            if (alert == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Unknown scenario '" + scenario + "'",
                    "validScenarios", List.of("concentration", "harvest", "drift", "compliance", "all")
                ));
            }
            // Re-create with a fresh UUID and current timestamp so repeated calls work
            toSend = List.of(new HnwAlert(
                UUID.randomUUID().toString(),
                alert.clientId(),
                alert.alertType(),
                alert.severity(),
                alert.title(),
                alert.detail(),
                alert.ticker(),
                Instant.now(),
                false
            ));
        }

        int clients = sseRegistry.connectedClientCount();
        toSend.forEach(alert -> {
            log.info("[TEST] Broadcasting mock alert '{}' to {} SSE client(s)", alert.title(), clients);
            sseRegistry.broadcast("alert", alert);
        });

        return ResponseEntity.ok(Map.of(
            "broadcasted", toSend.size(),
            "scenario", scenario,
            "sseClientsNotified", clients
        ));
    }
}
