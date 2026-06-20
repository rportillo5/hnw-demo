package com.edwardjones.demo.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "holdings")
public class Holding {

    @Id
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private String name;

    @Column(name = "target_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetPct;

    @Column(name = "asset_class", nullable = false)
    private String assetClass;   // US_EQUITY | FIXED_INCOME | COMMODITY | REAL_ESTATE | CASH

    protected Holding() {
    }

    public Holding(UUID id, UUID portfolioId, String ticker, String name,
                    BigDecimal targetPct, String assetClass) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.ticker = ticker;
        this.name = name;
        this.targetPct = targetPct;
        this.assetClass = assetClass;
    }

    public UUID getId() { return id; }
    public UUID getPortfolioId() { return portfolioId; }
    public String getTicker() { return ticker; }
    public String getName() { return name; }
    public BigDecimal getTargetPct() { return targetPct; }
    public String getAssetClass() { return assetClass; }
}
