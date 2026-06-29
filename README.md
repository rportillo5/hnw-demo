# HNW Demo — Tax-Aware Rebalancing Dashboard

Full-stack demo application featuring Databricks live alerts, AI-powered explanations, and LLM response streaming. Built with Spring Boot (Java 21) backend and React / Vite frontend.

> See [ARCHITECTURE.md](./ARCHITECTURE.md) for the full system diagram and data flow documentation.

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| Java | 21+ | `java -version` |
| Gradle | 8+ (or use wrapper) | `./gradlew --version` |
| Node.js | 18+ | `node --version` |
| npm | 9+ | `npm --version` |
| Docker Desktop | latest | required for PostgreSQL |
| Databricks JDBC JAR | 2.6.36 | see below |

## Step 1 — Download the Databricks JDBC driver

The Databricks JDBC driver is not in Maven Central. Download it manually:

1. Go to: https://www.databricks.com/spark/jdbc-drivers-download
2. Download the JDBC 4.2 compatible JAR
3. Rename it to `DatabricksJDBC.jar`
4. Place it in the `libs/` directory at the project root:
   ```
   hnw-demo/
   └── libs/
       └── DatabricksJDBC.jar   ← here
   ```

## Step 2 — Configure environment variables

```bash
cp .env.template .env
# Edit .env and fill in your real values
```

Required values:
- `DATABRICKS_HOST` — from Databricks SQL Warehouse → Connection details → Server hostname
- `DATABRICKS_HTTP_PATH` — from Databricks SQL Warehouse → Connection details → HTTP path
- `DATABRICKS_CLIENT_ID` — your service principal Client ID
- `DATABRICKS_CLIENT_SECRET` — your service principal Secret

## Step 3 — Start PostgreSQL locally

```bash
# Using Docker (simplest):
docker run -d \
  --name taxdemo-postgres \
  -e POSTGRES_DB=taxdemo \
  -e POSTGRES_USER=taxdemo \
  -e POSTGRES_PASSWORD=taxdemo_secret \
  -p 5432:5432 \
  postgres:16

# Verify it's running:
docker ps | grep taxdemo-postgres
```

## Step 4 — Build and run the backend

```bash
# Load environment variables and run
export $(cat .env | xargs) && ./gradlew bootRun
```

The backend starts on http://localhost:8080

## Step 5 — Install and run the frontend

```bash
cd frontend
npm install        # first time only
npm run dev
```

The frontend starts on http://localhost:5173

Open http://localhost:5173 in your browser to use the dashboard.

## Step 6 — Verify Databricks connectivity

```bash
# Run only the Databricks connectivity tests
export $(cat .env | xargs) && ./gradlew test --tests "*DatabricksConnectivityTest"
```

All 3 tests should pass:
- `databricks_select1_returns_true`
- `hnw_alerts_table_is_readable`
- `alert_polling_service_testConnection_returns_true`

## Step 7 — Verify SSE is working

Once the app is running:

```bash
# Open an SSE stream in a terminal
curl -N http://localhost:8080/api/alerts/stream?clientId=test-001

# In another terminal — check Databricks health
curl http://localhost:8080/api/alerts/health
# Expected: {"databricksConnected":true,"sseClientsConnected":1}

# Then run notebook 04_live_demo_alert.py in Databricks
# You should see the alert appear in the curl terminal within 10 seconds
```

## Project structure

### Backend
```
src/main/java/com/edwardjones/demo/
├── HnwDemoApplication.java
├── config/
│   ├── VirtualThreadConfig.java    ← executor bean
│   ├── PostgresConfig.java         ← primary DataSource (PostgreSQL)
│   └── DatabricksConfig.java       ← JDBC DataSource (Databricks)
├── alert/
│   ├── HnwAlert.java               ← record (Databricks row)
│   ├── SseEmitterRegistry.java     ← manages SSE connections
│   ├── AlertPollingService.java    ← virtual thread Databricks poller
│   └── AlertController.java        ← /api/alerts/* endpoints
├── domain/                         ← Client, Portfolio, Holding, TaxLot
├── tax/                            ← TaxRulesEngine, WhatIfCalculator
├── ai/                             ← TaxExplanationService, IntentRouter, AnthropicClient
└── api/                            ← PortfolioController, AssistantController
```

### Frontend
```
frontend/src/
├── App.tsx                         ← root component, routing, SSE wiring
├── components/
│   ├── ClientSplashPage.tsx        ← landing page with portfolio summary
│   ├── HarvestOpportunitiesList.tsx← tax-loss harvest candidates
│   ├── AllocationDriftPanel.tsx    ← drift chart with 5/25 Rule explainer
│   ├── AssistantQueryBox.tsx       ← query input + chip cards
│   ├── AiExplanationPanel.tsx      ← LLM streaming output (DOM ref)
│   ├── WhatIfPanel.tsx             ← tax calculation results + formula
│   ├── AlertTray.tsx               ← live Databricks alert notifications
│   ├── TopBar.tsx
│   └── Sidebar.tsx
├── hooks/
│   ├── useAssistant.ts             ← SSE streaming, intent routing
│   ├── useAlerts.ts                ← SSE alert subscription
│   └── usePortfolio.ts             ← harvest + drift data fetching
└── types/
    └── api.ts                      ← shared TypeScript interfaces
```

## Useful endpoints

### Backend (port 8080)
| Endpoint | Description |
|---|---|
| `GET /api/alerts/health` | Databricks connectivity + SSE client count |
| `GET /api/alerts/stream?clientId=xxx` | SSE stream for React AlertTray |
| `PATCH /api/alerts/{id}/read` | Mark alert as dismissed |
| `GET /api/portfolios/{id}/harvest-opportunities` | Tax-loss harvest candidates |
| `GET /api/portfolios/{id}/drift` | Allocation drift vs targets |
| `POST /api/assistant/stream` | AI assistant SSE streaming endpoint |
| `GET /actuator/health` | Spring Boot health (liveness + readiness) |

### Frontend (port 5173)
| URL | Description |
|---|---|
| `http://localhost:5173/` | Dashboard (splash page) |
