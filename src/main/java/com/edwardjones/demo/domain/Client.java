package com.edwardjones.demo.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String tier;   // "HNW", "MASS_AFFLUENT", etc.

    @Column(name = "aum", nullable = false, precision = 19, scale = 2)
    private BigDecimal aum;

    protected Client() {
        // JPA requires a no-arg constructor
    }

    public Client(UUID id, String name, String tier, BigDecimal aum) {
        this.id = id;
        this.name = name;
        this.tier = tier;
        this.aum = aum;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getTier() { return tier; }
    public BigDecimal getAum() { return aum; }
}
