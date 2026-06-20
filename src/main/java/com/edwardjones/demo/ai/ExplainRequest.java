package com.edwardjones.demo.ai;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for POST /api/explain
 */
public record ExplainRequest(
        @NotNull UUID lotId,
        String context   // optional free-text hint, e.g. "harvest" — nullable
) {
}
