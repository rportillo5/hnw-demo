package com.edwardjones.demo.tax;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for POST /api/portfolios/{portfolioId}/what-if
 *
 * Example:
 *   { "lotId": "30000000-0000-0000-0000-000000000001", "replacementTicker": "VTI" }
 */
public record WhatIfRequest(
        @NotNull UUID lotId,
        @NotBlank String replacementTicker
) {
}
