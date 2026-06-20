package com.edwardjones.demo.tax;

import java.math.BigDecimal;

/**
 * Result of a "what if I sell this lot and buy X instead" calculation.
 * Returned by POST /api/portfolios/{id}/what-if
 */
public record WhatIfResult(
        BigDecimal realizedGainLoss,
        BigDecimal estimatedTax,
        BigDecimal afterTaxProceeds,
        boolean washSaleWarning,
        String washSaleExplanation   // null if washSaleWarning is false
) {
}
