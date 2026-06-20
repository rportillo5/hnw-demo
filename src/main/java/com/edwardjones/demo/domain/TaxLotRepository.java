package com.edwardjones.demo.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaxLotRepository extends JpaRepository<TaxLot, UUID> {

    List<TaxLot> findByHoldingId(UUID holdingId);

    /**
     * Find all tax lots belonging to holdings within a given portfolio.
     * Used by PortfolioScanService to scan an entire portfolio's lots at once.
     */
    List<TaxLot> findByHoldingIdIn(List<UUID> holdingIds);
}
