package com.edwardjones.demo.tax;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Drift between a holding's current allocation % and its target %.
 * Positive driftPct = over-weight. Negative driftPct = under-weight.
 */
public record DriftResult(
        UUID holdingId,
        String ticker,
        BigDecimal currentPct,
        BigDecimal targetPct,
        BigDecimal driftPct
) {
    public boolean isOverWeight() {
        return driftPct.signum() > 0;
    }

    public boolean isUnderWeight() {
        return driftPct.signum() < 0;
    }
}
