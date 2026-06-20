package com.edwardjones.demo.tax;

import com.edwardjones.demo.domain.TaxLot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WhatIfTaxCalculator. The TaxLotRepository is mocked since
 * this is pure orchestration logic — no real database needed.
 */
@ExtendWith(MockitoExtension.class)
class WhatIfTaxCalculatorTest {

    @Mock
    private com.edwardjones.demo.domain.TaxLotRepository taxLotRepository;

    private WhatIfTaxCalculator calculator;
    private final TaxRulesEngine taxRulesEngine = new TaxRulesEngine();

    private static final UUID TSLA_LOT_A_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        calculator = new WhatIfTaxCalculator(taxLotRepository, taxRulesEngine);
    }

    @Test
    @DisplayName("TSLA Lot A sold, replaced with VTI -> loss, savings, no wash sale")
    void tslaLotA_replacedWithVti_noWashSale() {
        // TSLA Lot A: 50 shares, cost 245.00, current 178.50, purchased >365 days ago
        TaxLot tslaLotA = new TaxLot(
            TSLA_LOT_A_ID, UUID.randomUUID(), "TSLA",
            LocalDate.of(2023, 3, 15), new BigDecimal("50"),
            new BigDecimal("245.00"), new BigDecimal("178.50"), "Lot A"
        );
        when(taxLotRepository.findById(TSLA_LOT_A_ID)).thenReturn(Optional.of(tslaLotA));

        WhatIfResult result = calculator.calculate(TSLA_LOT_A_ID, "VTI");

        // gain/loss = (178.50 - 245.00) * 50 = -3325.00
        assertThat(result.realizedGainLoss()).isEqualByComparingTo(new BigDecimal("-3325.00"));

        // estimated tax = -(3325.00 * 0.238) = -791.35 (negative = savings)
        assertThat(result.estimatedTax()).isEqualByComparingTo(new BigDecimal("-791.35"));

        // after-tax proceeds for a loss = full market value = 178.50 * 50 = 8925.00
        assertThat(result.afterTaxProceeds()).isEqualByComparingTo(new BigDecimal("8925.00"));

        assertThat(result.washSaleWarning()).isFalse();
        assertThat(result.washSaleExplanation()).isNull();
    }

    @Test
    @DisplayName("TSLA Lot A sold, replaced with TSLA -> wash sale flagged")
    void tslaLotA_replacedWithTsla_washSaleFlagged() {
        TaxLot tslaLotA = new TaxLot(
            TSLA_LOT_A_ID, UUID.randomUUID(), "TSLA",
            LocalDate.of(2023, 3, 15), new BigDecimal("50"),
            new BigDecimal("245.00"), new BigDecimal("178.50"), "Lot A"
        );
        when(taxLotRepository.findById(TSLA_LOT_A_ID)).thenReturn(Optional.of(tslaLotA));

        WhatIfResult result = calculator.calculate(TSLA_LOT_A_ID, "TSLA");

        assertThat(result.washSaleWarning()).isTrue();
        assertThat(result.washSaleExplanation()).isNotNull();
        assertThat(result.washSaleExplanation()).contains("TSLA");
    }

    @Test
    @DisplayName("Replacement ticker check is case-insensitive (lowercase input)")
    void replacementTicker_caseInsensitive() {
        TaxLot tslaLotA = new TaxLot(
            TSLA_LOT_A_ID, UUID.randomUUID(), "TSLA",
            LocalDate.of(2023, 3, 15), new BigDecimal("50"),
            new BigDecimal("245.00"), new BigDecimal("178.50"), "Lot A"
        );
        when(taxLotRepository.findById(TSLA_LOT_A_ID)).thenReturn(Optional.of(tslaLotA));

        WhatIfResult result = calculator.calculate(TSLA_LOT_A_ID, "tsla");

        assertThat(result.washSaleWarning()).isTrue();
    }

    @Test
    @DisplayName("Lot with unrealized gain -> positive estimated tax owed, no savings")
    void lotWithGain_positiveTaxOwed() {
        UUID gainLotId = UUID.randomUUID();
        // AAPL: 100 shares, cost 145.00, current 195.00, long-term gain
        TaxLot aaplLot = new TaxLot(
            gainLotId, UUID.randomUUID(), "AAPL",
            LocalDate.of(2021, 5, 10), new BigDecimal("100"),
            new BigDecimal("145.00"), new BigDecimal("195.00"), "Lot A"
        );
        when(taxLotRepository.findById(gainLotId)).thenReturn(Optional.of(aaplLot));

        WhatIfResult result = calculator.calculate(gainLotId, "VTI");

        // gain = (195-145)*100 = 5000, tax = 5000 * 0.238 = 1190.00 (positive = owed)
        assertThat(result.realizedGainLoss()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.estimatedTax()).isEqualByComparingTo(new BigDecimal("1190.00"));
        assertThat(result.washSaleWarning()).isFalse();
    }

    @Test
    @DisplayName("Unknown lotId throws NoSuchElementException")
    void unknownLotId_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(taxLotRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calculator.calculate(unknownId, "VTI"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining(unknownId.toString());
    }
}
