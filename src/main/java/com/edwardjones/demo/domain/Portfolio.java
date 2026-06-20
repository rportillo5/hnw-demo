package com.edwardjones.demo.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(nullable = false)
    private String name;

    @Column(name = "account_type", nullable = false)
    private String accountType;   // TAXABLE | TRADITIONAL_IRA | ROTH_IRA | TRUST

    protected Portfolio() {
    }

    public Portfolio(UUID id, UUID clientId, String name, String accountType) {
        this.id = id;
        this.clientId = clientId;
        this.name = name;
        this.accountType = accountType;
    }

    public UUID getId() { return id; }
    public UUID getClientId() { return clientId; }
    public String getName() { return name; }
    public String getAccountType() { return accountType; }

    /**
     * Tax-loss harvesting is only meaningful in taxable accounts.
     * IRAs and Roth accounts don't recognize capital gains/losses.
     */
    public boolean isTaxable() {
        return "TAXABLE".equals(accountType) || "TRUST".equals(accountType);
    }
}
