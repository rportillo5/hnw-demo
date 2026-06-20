package com.edwardjones.demo.tax;

import java.math.BigDecimal;

/**
 * Result of classifying a single TaxLot.
 *
 * @param period       SHORT_TERM or LONG_TERM
 * @param gainLoss     Unrealized gain (positive) or loss (negative)
 * @param daysHeld     Number of days since purchase
 */
public record TaxClassification(
        HoldingPeriod period,
        BigDecimal gainLoss,
        long daysHeld
) {
    public boolean isLoss() {
        return gainLoss.signum() < 0;
    }

    public boolean isGain() {
        return gainLoss.signum() > 0;
    }
}
