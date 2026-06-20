package com.edwardjones.demo.tax;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single tax-loss harvest candidate, ranked by estimated tax savings.
 * Returned by GET /api/portfolios/{id}/harvest-opportunities
 */
public record HarvestOpportunity(
        UUID lotId,
        String ticker,
        String lotLabel,
        BigDecimal unrealizedLoss,        // always negative
        BigDecimal estimatedTaxSavings,   // always positive
        long daysHeld,
        HoldingPeriod period
) implements Comparable<HarvestOpportunity> {

    /**
     * Sort by largest tax savings first (descending).
     */
    @Override
    public int compareTo(HarvestOpportunity other) {
        return other.estimatedTaxSavings.compareTo(this.estimatedTaxSavings);
    }
}
