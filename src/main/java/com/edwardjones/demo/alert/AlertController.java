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

/**
 * REST + SSE endpoints for HNW financial alerts sourced from Databricks.
 *
 * GET  /api/alerts/stream?clientId=xxx  — SSE stream (React EventSource)
 * PATCH /api/alerts/{alertId}/read      — mark alert as read in Databricks
 * GET  /api/alerts/health               — Databricks connectivity check
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
}
