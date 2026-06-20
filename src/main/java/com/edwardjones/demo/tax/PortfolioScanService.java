package com.edwardjones.demo.tax;

import com.edwardjones.demo.domain.Holding;
import com.edwardjones.demo.domain.HoldingRepository;
import com.edwardjones.demo.domain.TaxLot;
import com.edwardjones.demo.domain.TaxLotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Scans an entire portfolio's holdings and tax lots concurrently using
 * virtual threads. Each holding's "fetch + classify" work is I/O-bound
 * in a real system (price lookups, lot classification calls) — virtual
 * threads let us fan out across all holdings without managing a thread pool.
 *
 * Demo talking point: "A 50-holding portfolio scan dispatches 50 virtual
 * threads instantly. On platform threads we'd be tuning a pool size;
 * here there's nothing to tune."
 */
@Service
public class PortfolioScanService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioScanService.class);

    private final HoldingRepository holdingRepository;
    private final TaxLotRepository taxLotRepository;
    private final TaxRulesEngine taxRulesEngine;
    private final ExecutorService virtualThreadExecutor;

    public PortfolioScanService(
            HoldingRepository holdingRepository,
            TaxLotRepository taxLotRepository,
            TaxRulesEngine taxRulesEngine,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor) {
        this.holdingRepository = holdingRepository;
        this.taxLotRepository = taxLotRepository;
        this.taxRulesEngine = taxRulesEngine;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    /**
     * Returns harvest opportunities across every holding in the portfolio,
     * ranked by estimated tax savings (largest first). Only lots with an
     * unrealized loss are included.
     */
    public List<HarvestOpportunity> findHarvestOpportunities(UUID portfolioId) {
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        log.info("Scanning {} holdings for harvest opportunities (portfolio {})",
            holdings.size(), portfolioId);

        // Dispatch one virtual thread per holding to fetch + evaluate its lots
        List<Future<List<HarvestOpportunity>>> futures = holdings.stream()
            .map(holding -> virtualThreadExecutor.submit(
                (Callable<List<HarvestOpportunity>>) () -> evaluateHoldingForHarvest(holding)
            ))
            .collect(Collectors.toList());

        List<HarvestOpportunity> allOpportunities = futures.stream()
            .map(this::resolveFuture)
            .flatMap(List::stream)
            .sorted()
            .collect(Collectors.toList());

        log.info("Found {} harvest opportunities across {} holdings",
            allOpportunities.size(), holdings.size());

        return allOpportunities;
    }

    private List<HarvestOpportunity> evaluateHoldingForHarvest(Holding holding) {
        List<TaxLot> lots = taxLotRepository.findByHoldingId(holding.getId());

        return lots.stream()
            .map(lot -> {
                TaxClassification classification = taxRulesEngine.classify(lot);
                if (!classification.isLoss()) {
                    return null;  // only losses are harvest candidates
                }
                BigDecimal savings = taxRulesEngine.estimatedTaxSavings(classification);
                return new HarvestOpportunity(
                    lot.getId(),
                    lot.getTicker(),
                    lot.getLotLabel(),
                    classification.gainLoss(),
                    savings,
                    classification.daysHeld(),
                    classification.period()
                );
            })
            .filter(opp -> opp != null)
            .collect(Collectors.toList());
    }

    /**
     * Returns per-holding allocation drift: current % of portfolio vs target %.
     */
    public List<DriftResult> calculateDrift(UUID portfolioId) {
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);

        // First pass: compute total portfolio market value (needs all lots)
        List<Future<BigDecimal>> valueFutures = holdings.stream()
            .map(holding -> virtualThreadExecutor.submit(
                (Callable<BigDecimal>) () -> holdingMarketValue(holding)
            ))
            .collect(Collectors.toList());

        List<BigDecimal> holdingValues = valueFutures.stream()
            .map(this::resolveFuture)
            .collect(Collectors.toList());

        BigDecimal totalValue = holdingValues.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.signum() == 0) {
            log.warn("Portfolio {} has zero total market value — cannot calculate drift", portfolioId);
            return List.of();
        }

        // Second pass: compute drift for each holding now that we know the total
        List<DriftResult> results = new java.util.ArrayList<>();
        for (int i = 0; i < holdings.size(); i++) {
            Holding holding = holdings.get(i);
            BigDecimal holdingValue = holdingValues.get(i);

            BigDecimal currentPct = holdingValue
                .multiply(BigDecimal.valueOf(100))
                .divide(totalValue, 2, RoundingMode.HALF_UP);

            BigDecimal driftPct = currentPct.subtract(holding.getTargetPct());

            results.add(new DriftResult(
                holding.getId(),
                holding.getTicker(),
                currentPct,
                holding.getTargetPct(),
                driftPct
            ));
        }

        return results;
    }

    private BigDecimal holdingMarketValue(Holding holding) {
        List<TaxLot> lots = taxLotRepository.findByHoldingId(holding.getId());
        return lots.stream()
            .map(TaxLot::currentMarketValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <T> T resolveFuture(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Portfolio scan interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Portfolio scan task failed", e.getCause());
        }
    }
}
