package com.edwardjones.demo.alert;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Polls workspace.default.hnw_alerts in Databricks every N seconds
 * using a Java 21 virtual thread. New (unread) alerts are broadcast
 * to all connected React clients via SSE.
 *
 * Demo talking point: "This virtual thread sits idle between polls —
 * it costs almost nothing while waiting, unlike a platform thread
 * which would block an OS thread the entire time."
 */
@Service
public class AlertPollingService {

    private static final Logger log = LoggerFactory.getLogger(AlertPollingService.class);

    private final DataSource databricksDataSource;
    private final SseEmitterRegistry sseRegistry;
    private final ExecutorService virtualThreadExecutor;

    @Value("${databricks.catalog:workspace}")
    private String catalog;

    @Value("${databricks.schema:default}")
    private String schema;

    @Value("${databricks.poll-interval-seconds:10}")
    private int pollIntervalSeconds;

    // Tracks the most recent alert we've seen — prevents re-broadcasting on restart
    private volatile Instant lastSeenAt = Instant.now().minusSeconds(300); // look back 5 min on startup

    public AlertPollingService(
            @Qualifier("databricksDataSource") DataSource databricksDataSource,
            SseEmitterRegistry sseRegistry,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor) {
        this.databricksDataSource = databricksDataSource;
        this.sseRegistry = sseRegistry;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @PostConstruct
    public void startPolling() {
        log.info("Starting Databricks alert polling (interval: {}s, table: {}.{}.hnw_alerts)",
            pollIntervalSeconds, catalog, schema);
        virtualThreadExecutor.submit(this::pollLoop);
    }

    // ── Poll loop ─────────────────────────────────────────────────────────────

    private void pollLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<HnwAlert> newAlerts = fetchNewAlerts();

                if (!newAlerts.isEmpty()) {
                    log.info("Databricks poll: {} new alert(s) found", newAlerts.size());
                    newAlerts.forEach(alert -> {
                        log.info("  → Broadcasting alert: [{}] {} — {}",
                            alert.severity(), alert.alertType(), alert.title());
                        sseRegistry.broadcast("alert", alert);
                    });

                    // Advance watermark to the latest alert we just processed
                    newAlerts.stream()
                        .map(HnwAlert::createdAt)
                        .max(Comparator.naturalOrder())
                        .ifPresent(latest -> lastSeenAt = latest);
                } else {
                    log.debug("Databricks poll: no new alerts (watermark: {})", lastSeenAt);
                }

                Thread.sleep(Duration.ofSeconds(pollIntervalSeconds));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Alert polling interrupted — shutting down");
            } catch (Exception e) {
                log.warn("Databricks poll error: {} — retrying in {}s",
                    e.getMessage(), pollIntervalSeconds);
                try {
                    Thread.sleep(Duration.ofSeconds(pollIntervalSeconds));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ── JDBC query ────────────────────────────────────────────────────────────

    private List<HnwAlert> fetchNewAlerts() throws SQLException {
        String sql = String.format("""
            SELECT alert_id, client_id, alert_type, severity,
                   title, detail, ticker, created_at, is_read
            FROM   %s.%s.hnw_alerts
            WHERE  created_at > ?
            AND    is_read = false
            ORDER  BY created_at ASC
            """, catalog, schema);

        try (Connection conn = databricksDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.from(lastSeenAt));
            ResultSet rs = ps.executeQuery();

            List<HnwAlert> alerts = new ArrayList<>();
            while (rs.next()) {
                alerts.add(HnwAlert.fromResultSet(rs));
            }
            return alerts;
        }
    }

    // ── Health check helper (used by connectivity test) ───────────────────────

    public boolean testConnection() {
        try (Connection conn = databricksDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            log.error("Databricks connectivity test failed: {}", e.getMessage());
            return false;
        }
    }
}
