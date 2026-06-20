package com.edwardjones.demo.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tax_lots")
public class TaxLot {

    @Id
    private UUID id;

    @Column(name = "holding_id", nullable = false)
    private UUID holdingId;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal shares;

    @Column(name = "cost_basis_per_share", nullable = false, precision = 19, scale = 4)
    private BigDecimal costBasisPerShare;

    @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "lot_label", nullable = false)
    private String lotLabel;   // "Lot A", "Lot B", etc. — for advisor-facing display

    protected TaxLot() {
    }

    public TaxLot(UUID id, UUID holdingId, String ticker, LocalDate purchaseDate,
                  BigDecimal shares, BigDecimal costBasisPerShare,
                  BigDecimal currentPrice, String lotLabel) {
        this.id = id;
        this.holdingId = holdingId;
        this.ticker = ticker;
        this.purchaseDate = purchaseDate;
        this.shares = shares;
        this.costBasisPerShare = costBasisPerShare;
        this.currentPrice = currentPrice;
        this.lotLabel = lotLabel;
    }

    public UUID getId() { return id; }
    public UUID getHoldingId() { return holdingId; }
    public String getTicker() { return ticker; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public BigDecimal getShares() { return shares; }
    public BigDecimal getCostBasisPerShare() { return costBasisPerShare; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public String getLotLabel() { return lotLabel; }

    /** Total unrealized gain/loss for this lot: (currentPrice - costBasis) * shares */
    public BigDecimal unrealizedGainLoss() {
        return currentPrice.subtract(costBasisPerShare).multiply(shares);
    }

    /** Total cost basis for this lot: costBasisPerShare * shares */
    public BigDecimal totalCostBasis() {
        return costBasisPerShare.multiply(shares);
    }

    /** Total current market value for this lot: currentPrice * shares */
    public BigDecimal currentMarketValue() {
        return currentPrice.multiply(shares);
    }
}
