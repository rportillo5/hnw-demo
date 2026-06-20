package com.edwardjones.demo.alert;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Immutable record representing one row from workspace.default.hnw_alerts.
 * Serialized as JSON and pushed to the React UI via SSE.
 */
public record HnwAlert(
        String alertId,
        String clientId,
        String alertType,   // CONCENTRATION | HARVEST_OPPORTUNITY | DRIFT | COMPLIANCE
        String severity,    // HIGH | MEDIUM | LOW
        String title,
        String detail,
        String ticker,      // nullable
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant createdAt,
        boolean isRead
) {
    /**
     * Factory method — maps a JDBC ResultSet row to an HnwAlert record.
     * Called from AlertPollingService after each Databricks poll.
     */
    public static HnwAlert fromResultSet(ResultSet rs) throws SQLException {
        return new HnwAlert(
            rs.getString("alert_id"),
            rs.getString("client_id"),
            rs.getString("alert_type"),
            rs.getString("severity"),
            rs.getString("title"),
            rs.getString("detail"),
            rs.getString("ticker"),          // may be null — that's fine
            rs.getTimestamp("created_at").toInstant(),
            rs.getBoolean("is_read")
        );
    }
}
