package com.edwardjones.demo.api;

import com.edwardjones.demo.tax.DriftResult;
import com.edwardjones.demo.tax.HarvestOpportunity;
import com.edwardjones.demo.tax.PortfolioScanService;
import com.edwardjones.demo.tax.WhatIfRequest;
import com.edwardjones.demo.tax.WhatIfResult;
import com.edwardjones.demo.tax.WhatIfTaxCalculator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolios")
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioScanService scanService;
    private final WhatIfTaxCalculator whatIfCalculator;

    public PortfolioController(PortfolioScanService scanService,
                                 WhatIfTaxCalculator whatIfCalculator) {
        this.scanService = scanService;
        this.whatIfCalculator = whatIfCalculator;
    }

    /**
     * Ranked tax-loss harvest opportunities for the given portfolio,
     * largest estimated tax savings first.
     */
    @GetMapping("/{portfolioId}/harvest-opportunities")
    public ResponseEntity<List<HarvestOpportunity>> harvestOpportunities(
            @PathVariable UUID portfolioId) {
        List<HarvestOpportunity> opportunities = scanService.findHarvestOpportunities(portfolioId);
        return ResponseEntity.ok(opportunities);
    }

    /**
     * Per-holding allocation drift vs target allocation.
     */
    @GetMapping("/{portfolioId}/drift")
    public ResponseEntity<List<DriftResult>> drift(@PathVariable UUID portfolioId) {
        List<DriftResult> driftResults = scanService.calculateDrift(portfolioId);
        return ResponseEntity.ok(driftResults);
    }

    /**
     * "What if I sell this lot and buy {replacementTicker} instead?"
     * Returns realized gain/loss, estimated tax impact, after-tax proceeds,
     * and a wash-sale warning if applicable.
     *
     * Note: portfolioId in the path is not currently used to scope the lookup
     * (lotId is globally unique) — kept in the URL to match REST resource
     * nesting conventions and for future portfolio-level validation.
     */
    @PostMapping("/{portfolioId}/what-if")
    public ResponseEntity<?> whatIf(
            @PathVariable UUID portfolioId,
            @Valid @RequestBody WhatIfRequest request) {
        try {
            WhatIfResult result = whatIfCalculator.calculate(
                request.lotId(), request.replacementTicker());
            return ResponseEntity.ok(result);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
