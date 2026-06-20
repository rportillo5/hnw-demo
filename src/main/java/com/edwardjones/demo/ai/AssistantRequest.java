package com.edwardjones.demo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for POST /api/assistant/query
 *
 * alertContext is optional — populated when the query originates
 * from clicking "Ask AI" on a Databricks AlertCard.
 */
public record AssistantRequest(
        @NotNull  UUID portfolioId,
        UUID currentLotId,          // nullable — not required for all intents
        @NotBlank String text,
        AlertContext alertContext    // nullable — only present for alert-driven queries
) {
    public record AlertContext(
            String alertId,
            String alertType,
            String ticker,
            String detail
    ) {}
}
