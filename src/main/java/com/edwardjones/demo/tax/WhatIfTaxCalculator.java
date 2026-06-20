package com.edwardjones.demo.tax;

import com.edwardjones.demo.domain.TaxLot;
import com.edwardjones.demo.domain.TaxLotRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Runs a "what if I sell this lot and buy a replacement ticker instead"
 * scenario. Pure orchestration over TaxRulesEngine — no new tax math here,
 * just wiring lot lookup + classification + wash-sale check together.
 */
@Service
public class WhatIfTaxCalculator {

    private final TaxLotRepository taxLotRepository;
    private final TaxRulesEngine taxRulesEngine;

    public WhatIfTaxCalculator(TaxLotRepository taxLotRepository, TaxRulesEngine taxRulesEngine) {
        this.taxLotRepository = taxLotRepository;
        this.taxRulesEngine = taxRulesEngine;
    }

    /**
     * @throws NoSuchElementException if lotId does not match any TaxLot
     */
    public WhatIfResult calculate(UUID lotId, String replacementTicker) {
        TaxLot lot = taxLotRepository.findById(lotId)
            .orElseThrow(() -> new NoSuchElementException("No tax lot found with id: " + lotId));

        TaxClassification classification = taxRulesEngine.classify(lot);

        boolean washSale = taxRulesEngine.isWashSale(lot.getTicker(), replacementTicker);
        String washSaleExplanation = washSale
            ? taxRulesEngine.washSaleExplanation(lot.getTicker(), replacementTicker)
            : null;

        // Tax treatment: a loss generates savings (estimatedTax is reported as a
        // negative figure, representing tax reduction), a gain generates tax owed
        // (positive figure). This mirrors the sign convention in 05-ai-explanation-service.md.
        var estimatedTax = classification.isLoss()
            ? taxRulesEngine.estimatedTaxSavings(classification).negate()
            : taxRulesEngine.estimatedTaxOwed(classification);

        var afterTaxProceeds = taxRulesEngine.afterTaxProceeds(lot, classification);

        return new WhatIfResult(
            classification.gainLoss(),
            estimatedTax,
            afterTaxProceeds,
            washSale,
            washSaleExplanation
        );
    }
}
