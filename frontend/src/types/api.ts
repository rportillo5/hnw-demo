// ── Databricks alert (from SSE stream) ───────────────────────────────────────

export interface HnwAlert {
  alertId: string
  clientId: string
  alertType: 'CONCENTRATION' | 'HARVEST_OPPORTUNITY' | 'DRIFT' | 'COMPLIANCE'
  severity: 'HIGH' | 'MEDIUM' | 'LOW'
  title: string
  detail: string
  ticker: string | null
  createdAt: string   // ISO timestamp
  isRead: boolean
}

// ── Portfolio data (from REST endpoints) ─────────────────────────────────────

export interface HarvestOpportunity {
  lotId: string
  ticker: string
  lotLabel: string
  unrealizedLoss: number
  estimatedTaxSavings: number
  daysHeld: number
  period: 'SHORT_TERM' | 'LONG_TERM'
}

export interface DriftResult {
  holdingId: string
  ticker: string
  currentPct: number
  targetPct: number
  driftPct: number
}

// ── AI explanation ────────────────────────────────────────────────────────────

export interface ExplainResponse {
  explanation: string
  disclaimer: string
}

// ── What-if calculation ───────────────────────────────────────────────────────

export interface WhatIfResult {
  realizedGainLoss: number
  estimatedTax: number       // negative = savings, positive = owed
  afterTaxProceeds: number
  washSaleWarning: boolean
  washSaleExplanation: string | null
}

// ── Databricks health ─────────────────────────────────────────────────────────

export interface DatabricksHealth {
  databricksConnected: boolean
  sseClientsConnected: number
}
