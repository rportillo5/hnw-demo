# Day 3 — Setup Instructions

## What's in this delivery

```
src/main/java/com/edwardjones/demo/
├── domain/
│   ├── Client.java              (NEW)
│   ├── ClientRepository.java    (NEW)
│   ├── Portfolio.java           (NEW)
│   ├── PortfolioRepository.java (NEW)
│   ├── Holding.java             (NEW)
│   ├── HoldingRepository.java   (NEW)
│   ├── TaxLot.java              (NEW)
│   └── TaxLotRepository.java    (NEW)
├── tax/
│   ├── HoldingPeriod.java          (NEW)
│   ├── TaxClassification.java      (NEW)
│   ├── HarvestOpportunity.java     (NEW)
│   ├── DriftResult.java            (NEW)
│   ├── TaxRulesEngine.java         (NEW)
│   └── PortfolioScanService.java   (NEW)
└── api/
    └── PortfolioController.java    (NEW)

src/test/java/com/edwardjones/demo/tax/
└── TaxRulesEngineTest.java         (NEW)

seed.sql                            (NEW — PostgreSQL schema + data)
```

## Step 1 — Copy files into your existing project

Copy each folder above into the matching location in your `hnw-demo/` project
(the one with `AlertPollingService.java` etc. from Day 1-2). No existing files
are overwritten — these are all new files in new packages.

## Step 2 — Create the database schema and seed data

With PostgreSQL running (your existing `taxdemo-postgres` Docker container):

```zsh
psql -h localhost -U taxdemo -d taxdemo -f seed.sql
```

Enter password `taxdemo_secret` when prompted (or set `PGPASSWORD` env var
to skip the prompt).

You should see a verification table printed at the end showing all 10
holdings with lot counts and market values.

## Step 3 — Run the tests (no Databricks or PostgreSQL needed)

These are pure Java unit tests — they don't touch the database or Databricks
at all, so they'll run instantly:

```zsh
gradle test --tests "*TaxRulesEngineTest*"
```

All 13 tests should pass. This validates the entire tax logic checklist:
holding period classification, gain/loss detection, tax savings math,
wash-sale detection, and after-tax proceeds.

## Step 4 — Start the app and hit the new endpoints

```zsh
gradle bootRun
```

Once it's up, test the new endpoints (Margaret Holloway's Taxable Brokerage
portfolio ID is `10000000-0000-0000-0000-000000000001`):

```zsh
curl "http://localhost:8080/api/portfolios/10000000-0000-0000-0000-000000000001/harvest-opportunities" | python3 -m json.tool
```

Expected: TSLA Lot A/B/C and INTC Lot A/B appear as harvest opportunities,
sorted by estimated tax savings descending (TSLA Lot A should be near the top
— it's the biggest dollar loss).

```zsh
curl "http://localhost:8080/api/portfolios/10000000-0000-0000-0000-000000000001/drift" | python3 -m json.tool
```

Expected: 10 holdings, each showing currentPct vs targetPct vs driftPct.
TSLA and INTC should show negative drift (under-weight, since they've lost
value relative to target).

## A note on ddl-auto

Your `application.properties` (or `application-local.properties`) has:
```properties
spring.jpa.hibernate.ddl-auto=validate
```

This means Hibernate will check that your JPA entities match the existing
tables but won't create them — that's why running `seed.sql` first (Step 2)
is required. If you ever see a "Schema-validation: missing table" error,
it means `seed.sql` wasn't run yet.

## What's intentionally NOT included yet

- No what-if calculator endpoint — that's Day 4
- No AI explanation service — that's Day 5
- The harvest-opportunities and drift endpoints return raw JSON — frontend
  rendering comes Day 6-7
