package com.edwardjones.demo.tax;

import com.edwardjones.demo.domain.TaxLot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic tax calculation engine. Pure Java — no Spring context
 * required to unit test, no LLM calls, no external I/O.
 *
 * SIMPLIFICATIONS (documented per spec — do not "fix" without updating
 * the demo disclaimer language too):
 *   - Ignores Alternative Minimum Tax (AMT)
 *   - Ignores state income taxes
 *   - Long-term rate folds in Net Investment Income Tax (NIIT) as a flat add
 *   - Wash-sale detection uses a small hardcoded "substantially identical" list
 *   - Assumes top-bracket HNW client throughout (37% short-term, 23.8% long-term)
 */
@Component
public class TaxRulesEngine {

    // ── Simplified 2026 rate assumptions for HNW (top bracket) clients ────────
    static final BigDecimal SHORT_TERM_RATE = new BigDecimal("0.37");   // ordinary income top bracket
    static final BigDecimal LONG_TERM_RATE  = new BigDecimal("0.238");  // 20% LTCG + 3.8% NIIT, simplified flat

    private static final int LONG_TERM_THRESHOLD_DAYS = 365;

    // ── Simplified "substantially identical" pairs for wash-sale detection ────
    static final Map<String, Set<String>> WASH_SALE_PAIRS = Map.of(
        "TSLA", Set.of("TSLA"),
        "INTC", Set.of("INTC", "SOXX"),
        "VTI",  Set.of("VTI", "ITOT", "SCHB"),
        "BND",  Set.of("BND", "AGG", "SCHZ")
    );

    private static final int WASH_SALE_WINDOW_DAYS = 30;

    // ── Classification ──────────────────────────────────────────────────────

    /**
     * Classify a tax lot's holding period and unrealized gain/loss.
     */
    public TaxClassification classify(TaxLot lot) {
        long daysHeld = ChronoUnit.DAYS.between(lot.getPurchaseDate(), LocalDate.now());
        HoldingPeriod period = daysHeld > LONG_TERM_THRESHOLD_DAYS
            ? HoldingPeriod.LONG_TERM
            : HoldingPeriod.SHORT_TERM;
        BigDecimal gainLoss = lot.unrealizedGainLoss();
        return new TaxClassification(period, gainLoss, daysHeld);
    }

    /**
     * The applicable tax rate for a given holding period.
     * Simplified flat-rate assumption for HNW demo clients (see class javadoc).
     */
    public BigDecimal applicableRate(HoldingPeriod period) {
        return period == HoldingPeriod.LONG_TERM ? LONG_TERM_RATE : SHORT_TERM_RATE;
    }

    /**
     * Estimated tax savings from harvesting this lot's loss.
     * Only meaningful when gainLoss is negative (a loss).
     * Returns a positive number representing the dollar tax benefit.
     */
    public BigDecimal estimatedTaxSavings(TaxClassification classification) {
        if (!classification.isLoss()) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = applicableRate(classification.period());
        // gainLoss is negative; multiply by rate and negate to get a positive savings figure
        return classification.gainLoss().abs().multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Estimated tax owed on a realized gain. Returns ZERO if gainLoss is a loss
     * (losses don't generate tax owed — they generate the savings above instead).
     */
    public BigDecimal estimatedTaxOwed(TaxClassification classification) {
        if (!classification.isGain()) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = applicableRate(classification.period());
        return classification.gainLoss().multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    // ── Wash-sale detection ─────────────────────────────────────────────────

    /**
     * Check whether buying replacementTicker within WASH_SALE_WINDOW_DAYS
     * of selling soldTicker would trigger the wash-sale rule.
     *
     * Simplified: real wash-sale analysis covers "substantially identical"
     * securities per IRS guidance and requires legal/tax counsel — this is
     * a demo approximation using a small hardcoded list.
     */
    public boolean isWashSale(String soldTicker, String replacementTicker) {
        Set<String> identicalSet = WASH_SALE_PAIRS.get(soldTicker.toUpperCase());
        if (identicalSet == null) {
            return false;
        }
        return identicalSet.contains(replacementTicker.toUpperCase());
    }

    public String washSaleExplanation(String soldTicker, String replacementTicker) {
        return String.format(
            "Purchasing %s within %d days of selling %s may trigger the wash-sale rule, " +
            "which would disallow the loss deduction. Consider a different replacement security.",
            replacementTicker.toUpperCase(), WASH_SALE_WINDOW_DAYS, soldTicker.toUpperCase()
        );
    }

    // ── After-tax proceeds ──────────────────────────────────────────────────

    /**
     * After-tax proceeds from selling a lot.
     * For a loss: proceeds = market value (no tax owed; savings tracked separately).
     * For a gain: proceeds = market value - tax owed on the gain.
     */
    public BigDecimal afterTaxProceeds(TaxLot lot, TaxClassification classification) {
        BigDecimal marketValue = lot.currentMarketValue();
        if (classification.isGain()) {
            return marketValue.subtract(estimatedTaxOwed(classification));
        }
        return marketValue;
    }
}
