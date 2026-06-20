package com.edwardjones.demo.tax;

import com.edwardjones.demo.domain.TaxLot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure Java unit tests — no Spring context required.
 * Covers the checklist from 04-tax-rules-engine.md.
 */
class TaxRulesEngineTest {

    private TaxRulesEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TaxRulesEngine();
    }

    // ── Holding period classification ──────────────────────────────────────

    @Test
    @DisplayName("Lot held 200 days → SHORT_TERM classification")
    void shortTermLot_classifiedCorrectly() {
        TaxLot lot = lotHeldDaysAgo(200, new BigDecimal("100.00"), new BigDecimal("90.00"));

        TaxClassification result = engine.classify(lot);

        assertThat(result.period()).isEqualTo(HoldingPeriod.SHORT_TERM);
        assertThat(result.daysHeld()).isEqualTo(200);
    }

    @Test
    @DisplayName("Lot held 400 days → LONG_TERM classification")
    void longTermLot_classifiedCorrectly() {
        TaxLot lot = lotHeldDaysAgo(400, new BigDecimal("100.00"), new BigDecimal("90.00"));

        TaxClassification result = engine.classify(lot);

        assertThat(result.period()).isEqualTo(HoldingPeriod.LONG_TERM);
        assertThat(result.daysHeld()).isEqualTo(400);
    }

    @Test
    @DisplayName("Lot held exactly 365 days → SHORT_TERM (boundary: must exceed 365)")
    void boundaryAt365Days_isShortTerm() {
        TaxLot lot = lotHeldDaysAgo(365, new BigDecimal("100.00"), new BigDecimal("90.00"));

        TaxClassification result = engine.classify(lot);

        assertThat(result.period()).isEqualTo(HoldingPeriod.SHORT_TERM);
    }

    // ── Gain/loss detection ─────────────────────────────────────────────────

    @Test
    @DisplayName("Lot with unrealized loss → isLoss() true, isGain() false")
    void lotWithLoss_detectedCorrectly() {
        TaxLot lot = lotHeldDaysAgo(400, new BigDecimal("100.00"), new BigDecimal("80.00"));

        TaxClassification result = engine.classify(lot);

        assertThat(result.isLoss()).isTrue();
        assertThat(result.isGain()).isFalse();
        assertThat(result.gainLoss()).isNegative();
    }

    @Test
    @DisplayName("Lot with unrealized gain → isGain() true, isLoss() false")
    void lotWithGain_detectedCorrectly() {
        TaxLot lot = lotHeldDaysAgo(400, new BigDecimal("100.00"), new BigDecimal("120.00"));

        TaxClassification result = engine.classify(lot);

        assertThat(result.isGain()).isTrue();
        assertThat(result.isLoss()).isFalse();
        assertThat(result.gainLoss()).isPositive();
    }

    // ── Tax savings estimation ──────────────────────────────────────────────

    @Test
    @DisplayName("Long-term loss: savings = unrealizedLoss * 0.238")
    void longTermLossSavings_calculatedCorrectly() {
        // 100 shares, cost basis 100/share, current price 90/share → loss of $1000
        TaxLot lot = lotHeldDaysAgo(400, new BigDecimal("100.00"), new BigDecimal("90.00"),
            new BigDecimal("100"));

        TaxClassification classification = engine.classify(lot);
        BigDecimal savings = engine.estimatedTaxSavings(classification);

        // 1000 * 0.238 = 238.00
        assertThat(savings).isEqualByComparingTo(new BigDecimal("238.00"));
    }

    @Test
    @DisplayName("Short-term loss: savings = unrealizedLoss * 0.37")
    void shortTermLossSavings_calculatedCorrectly() {
        TaxLot lot = lotHeldDaysAgo(100, new BigDecimal("100.00"), new BigDecimal("90.00"),
            new BigDecimal("100"));

        TaxClassification classification = engine.classify(lot);
        BigDecimal savings = engine.estimatedTaxSavings(classification);

        // 1000 * 0.37 = 370.00
        assertThat(savings).isEqualByComparingTo(new BigDecimal("370.00"));
    }

    @Test
    @DisplayName("Gain produces zero tax savings (savings only applies to losses)")
    void gainProducesZeroSavings() {
        TaxLot lot = lotHeldDaysAgo(400, new BigDecimal("100.00"), new BigDecimal("120.00"));

        TaxClassification classification = engine.classify(lot);
        BigDecimal savings = engine.estimatedTaxSavings(classification);

        assertThat(savings).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Wash-sale detection ─────────────────────────────────────────────────

    @Test
    @DisplayName("TSLA sold, TSLA repurchased within window → wash sale flagged")
    void tslaToTsla_isWashSale() {
        assertThat(engine.isWashSale("TSLA", "TSLA")).isTrue();
    }

    @Test
    @DisplayName("TSLA sold, VTI repurchased → no wash sale")
    void tslaToVti_isNotWashSale() {
        assertThat(engine.isWashSale("TSLA", "VTI")).isFalse();
    }

    @Test
    @DisplayName("INTC sold, SOXX repurchased → wash sale (substantially identical)")
    void intcToSoxx_isWashSale() {
        assertThat(engine.isWashSale("INTC", "SOXX")).isTrue();
    }

    @Test
    @DisplayName("Wash sale check is case-insensitive")
    void washSaleCheck_caseInsensitive() {
        assertThat(engine.isWashSale("tsla", "TSLA")).isTrue();
        assertThat(engine.isWashSale("TSLA", "tsla")).isTrue();
    }

    @Test
    @DisplayName("Unknown ticker with no wash-sale pairs defined → no wash sale")
    void unknownTicker_noWashSale() {
        assertThat(engine.isWashSale("AAPL", "AAPL")).isFalse();
    }

    // ── After-tax proceeds ──────────────────────────────────────────────────

    @Test
    @DisplayName("After-tax proceeds for a loss equals full market value (no tax owed)")
    void afterTaxProceeds_forLoss_equalsMarketValue() {
        TaxLot lot = lotHeldDaysAgo(400, new BigDecimal("100.00"), new BigDecimal("90.00"),
            new BigDecimal("100"));
        TaxClassification classification = engine.classify(lot);

        BigDecimal proceeds = engine.afterTaxProceeds(lot, classification);

        // 100 shares * $90 = $9000 market value, no tax owed on a loss
        assertThat(proceeds).isEqualByComparingTo(new BigDecimal("9000.00"));
    }

    @Test
    @DisplayName("After-tax proceeds for a long-term gain subtracts estimated tax")
    void afterTaxProceeds_forLongTermGain_subtractsTax() {
        TaxLot lot = lotHeldDaysAgo(400, new BigDecimal("100.00"), new BigDecimal("110.00"),
            new BigDecimal("100"));
        TaxClassification classification = engine.classify(lot);

        BigDecimal proceeds = engine.afterTaxProceeds(lot, classification);

        // Market value: 100 * 110 = 11000
        // Gain: (110-100)*100 = 1000, tax = 1000 * 0.238 = 238
        // Proceeds: 11000 - 238 = 10762
        assertThat(proceeds).isEqualByComparingTo(new BigDecimal("10762.00"));
    }

    // ── Test helpers ──────────────────────────────────────────────────────────

    private TaxLot lotHeldDaysAgo(long daysAgo, BigDecimal costBasis, BigDecimal currentPrice) {
        return lotHeldDaysAgo(daysAgo, costBasis, currentPrice, BigDecimal.TEN);
    }

    private TaxLot lotHeldDaysAgo(long daysAgo, BigDecimal costBasis, BigDecimal currentPrice,
                                    BigDecimal shares) {
        return new TaxLot(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "TEST",
            LocalDate.now().minusDays(daysAgo),
            shares,
            costBasis,
            currentPrice,
            "Lot Test"
        );
    }
}
