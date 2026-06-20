-- =============================================================================
-- Day 3 schema + seed data
-- Run this against your local PostgreSQL (taxdemo database) before starting
-- the Spring Boot app, OR let Hibernate create tables (ddl-auto=validate
-- requires tables to already exist, so run this script first either way).
--
-- Usage:
--   psql -h localhost -U taxdemo -d taxdemo -f seed.sql
-- =============================================================================

-- ── Schema ───────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS clients (
    id    UUID PRIMARY KEY,
    name  VARCHAR(255) NOT NULL,
    tier  VARCHAR(50)  NOT NULL,
    aum   NUMERIC(19,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS portfolios (
    id           UUID PRIMARY KEY,
    client_id    UUID NOT NULL REFERENCES clients(id),
    name         VARCHAR(255) NOT NULL,
    account_type VARCHAR(50)  NOT NULL
);

CREATE TABLE IF NOT EXISTS holdings (
    id           UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios(id),
    ticker       VARCHAR(10)  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    target_pct   NUMERIC(5,2) NOT NULL,
    asset_class  VARCHAR(50)  NOT NULL
);

CREATE TABLE IF NOT EXISTS tax_lots (
    id                    UUID PRIMARY KEY,
    holding_id            UUID NOT NULL REFERENCES holdings(id),
    ticker                VARCHAR(10)  NOT NULL,
    purchase_date         DATE NOT NULL,
    shares                NUMERIC(19,4) NOT NULL,
    cost_basis_per_share  NUMERIC(19,4) NOT NULL,
    current_price         NUMERIC(19,4) NOT NULL,
    lot_label             VARCHAR(50)  NOT NULL
);

-- ── Seed data — clears existing rows first for repeatable runs ────────────────

DELETE FROM tax_lots;
DELETE FROM holdings;
DELETE FROM portfolios;
DELETE FROM clients;

-- Client: Margaret Holloway (matches the seed used in Databricks notebooks)
INSERT INTO clients (id, name, tier, aum) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Margaret Holloway', 'HNW', 4250000.00);

-- Portfolios
INSERT INTO portfolios (id, client_id, name, account_type) VALUES
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Taxable Brokerage', 'TAXABLE'),
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Traditional IRA', 'TRADITIONAL_IRA');

-- Holdings (all in the Taxable Brokerage portfolio)
INSERT INTO holdings (id, portfolio_id, ticker, name, target_pct, asset_class) VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'AAPL', 'Apple Inc.',              18.00, 'US_EQUITY'),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'MSFT', 'Microsoft Corp.',         15.00, 'US_EQUITY'),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'VTI',  'Vanguard Total Market',  20.00, 'US_EQUITY'),
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'BND',  'Vanguard Total Bond',     8.00, 'FIXED_INCOME'),
    ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'GLD',  'SPDR Gold Shares',        5.00, 'COMMODITY'),
    ('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', 'AMZN', 'Amazon.com Inc.',        12.00, 'US_EQUITY'),
    ('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', 'TSLA', 'Tesla Inc.',              7.00, 'US_EQUITY'),
    ('20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', 'INTC', 'Intel Corp.',             5.00, 'US_EQUITY'),
    ('20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000001', 'VNQ',  'Vanguard Real Estate',    5.00, 'REAL_ESTATE'),
    ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001', 'CASH', 'Cash & Equivalents',      5.00, 'CASH');

-- Tax lots — TSLA (key demo story: harvest candidate)
INSERT INTO tax_lots (id, holding_id, ticker, purchase_date, shares, cost_basis_per_share, current_price, lot_label) VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000007', 'TSLA', '2023-03-15', 50, 245.00, 178.50, 'Lot A'),
    ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000007', 'TSLA', '2024-01-08', 30, 195.00, 178.50, 'Lot B'),
    ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000007', 'TSLA', '2024-09-22', 20, 152.00, 178.50, 'Lot C');

-- Tax lots — INTC (secondary harvest candidate)
INSERT INTO tax_lots (id, holding_id, ticker, purchase_date, shares, cost_basis_per_share, current_price, lot_label) VALUES
    ('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000008', 'INTC', '2022-11-10', 200, 41.50, 22.30, 'Lot A'),
    ('30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000008', 'INTC', '2023-06-01', 100, 35.00, 22.30, 'Lot B');

-- Tax lots — remaining holdings (single lot each, all gains, for portfolio completeness)
INSERT INTO tax_lots (id, holding_id, ticker, purchase_date, shares, cost_basis_per_share, current_price, lot_label) VALUES
    ('30000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000001', 'AAPL', '2021-05-10', 100, 145.00, 195.00, 'Lot A'),
    ('30000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000002', 'MSFT', '2021-08-12', 80,  280.00, 415.00, 'Lot A'),
    ('30000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000003', 'VTI',  '2022-02-20', 150, 195.00, 265.00, 'Lot A'),
    ('30000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000004', 'BND',  '2022-06-15', 300, 72.00,  68.50,  'Lot A'),
    ('30000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000005', 'GLD',  '2023-01-05', 60,  175.00, 215.00, 'Lot A'),
    ('30000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000006', 'AMZN', '2022-09-30', 70,  110.00, 178.00, 'Lot A'),
    ('30000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000009', 'VNQ',  '2022-11-18', 120, 78.00,  86.00,  'Lot A');

-- Verification query
SELECT
    h.ticker,
    h.target_pct,
    COUNT(tl.id) AS lot_count,
    SUM(tl.shares * tl.current_price) AS market_value
FROM holdings h
LEFT JOIN tax_lots tl ON tl.holding_id = h.id
GROUP BY h.ticker, h.target_pct
ORDER BY h.ticker;
