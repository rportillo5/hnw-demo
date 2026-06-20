import { HarvestOpportunity, DriftResult } from '../types/api'

interface ClientSplashPageProps {
  opportunities: HarvestOpportunity[]
  drift: DriftResult[]
  loading: boolean
  alertCount: number
  onViewPortfolio: () => void
  onViewAssistant: () => void
}

export default function ClientSplashPage({
  opportunities,
  drift,
  loading,
  alertCount,
  onViewPortfolio,
  onViewAssistant,
}: ClientSplashPageProps) {

  const totalLoss = opportunities.reduce((sum, o) => sum + o.unrealizedLoss, 0)
  const totalSavings = opportunities.reduce((sum, o) => sum + o.estimatedTaxSavings, 0)
  const maxDrift = drift.length > 0
    ? Math.max(...drift.map(d => Math.abs(d.driftPct)))
    : 0

  const highAlerts = opportunities.length > 0 ? alertCount : 0

  return (
    <div className="min-h-full p-8 flex flex-col items-center justify-start"
      style={{ background: 'var(--surface)' }}>

      <div className="w-full max-w-2xl">

        {/* Page header */}
        <div className="mb-6">
          <p className="text-xs font-semibold uppercase tracking-widest mb-1"
            style={{ color: 'var(--ej-blue)' }}>
            Client Overview
          </p>
          <h1 className="text-2xl font-semibold text-gray-900">
            Good morning, advisor.
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Here's where things stand for your client today.
          </p>
        </div>

        {/* Client profile card */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 mb-5">
          <div className="flex items-start gap-4">

            {/* Avatar */}
            <div
              className="w-14 h-14 rounded-full flex items-center justify-center text-lg font-semibold flex-shrink-0"
              style={{ backgroundColor: '#E6F1FF', color: 'var(--ej-blue)' }}
            >
              MH
            </div>

            {/* Client info */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap mb-1">
                <h2 className="text-lg font-semibold text-gray-900">
                  Margaret Holloway
                </h2>
                <span
                  className="text-xs font-medium px-2.5 py-0.5 rounded-full"
                  style={{ background: '#EEEDFE', color: '#3C3489' }}
                >
                  HNW
                </span>
                <span
                  className="text-xs font-medium px-2.5 py-0.5 rounded-full"
                  style={{ background: '#E1F5EE', color: '#0F6E56' }}
                >
                  Active Client
                </span>
              </div>

              <p className="text-xs text-gray-400 mb-4">
                Taxable Brokerage · Traditional IRA · Trust
              </p>

              {/* Client quick stats */}
              <div className="flex gap-6 flex-wrap">
                <div>
                  <p className="text-xs text-gray-400 mb-0.5">AUM</p>
                  <p className="text-base font-semibold text-gray-900">$4,250,000</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400 mb-0.5">Advisor</p>
                  <p className="text-base font-semibold text-gray-900">J. Patterson</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400 mb-0.5">Last review</p>
                  <p className="text-base font-semibold text-gray-900">Mar 2026</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400 mb-0.5">Risk profile</p>
                  <p className="text-base font-semibold text-gray-900">Moderate Growth</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* At-a-glance stats */}
        <div className="grid grid-cols-3 gap-4 mb-5">
          <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4">
            <p className="text-xs text-gray-400 mb-1">Harvest opportunity</p>
            {loading
              ? <div className="skeleton h-7 w-24 mb-1" />
              : <p className="text-2xl font-semibold"
                  style={{ color: 'var(--loss-red)' }}>
                  ${Math.abs(totalLoss).toLocaleString('en-US', { maximumFractionDigits: 0 })}
                </p>
            }
            <p className="text-xs text-gray-400">
              {loading ? '' : `${opportunities.length} harvest candidates`}
            </p>
          </div>

          <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4">
            <p className="text-xs text-gray-400 mb-1">Est. tax savings</p>
            {loading
              ? <div className="skeleton h-7 w-20 mb-1" />
              : <p className="text-2xl font-semibold"
                  style={{ color: 'var(--gain-green)' }}>
                  ${totalSavings.toLocaleString('en-US', { maximumFractionDigits: 0 })}
                </p>
            }
            <p className="text-xs text-gray-400">if all losses harvested</p>
          </div>

          <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4">
            <p className="text-xs text-gray-400 mb-1">Allocation drift</p>
            {loading
              ? <div className="skeleton h-7 w-16 mb-1" />
              : <p className="text-2xl font-semibold"
                  style={{ color: '#D97706' }}>
                  {maxDrift.toFixed(1)}%
                </p>
            }
            <p className="text-xs text-gray-400">max from target</p>
          </div>
        </div>

        {/* Databricks alert teaser */}
        {alertCount > 0 && (
          <div
            className="rounded-xl p-4 mb-6 flex items-center gap-3"
            style={{
              background: '#FAEEDA',
              border: '0.5px solid #FAC775',
            }}
          >
            <span className="text-base" role="img" aria-label="bell">🔔</span>
            <div>
              <span className="text-sm font-semibold" style={{ color: '#633806' }}>
                {alertCount} active alert{alertCount !== 1 ? 's' : ''} from Databricks
              </span>
              <span className="text-sm" style={{ color: '#854F0B' }}>
                {' '}— including a HIGH severity TSLA concentration flag
              </span>
            </div>
            <span
              className="ml-auto text-xs font-medium px-2 py-1 rounded-full flex-shrink-0"
              style={{ background: '#FF3621', color: 'white' }}
            >
              Live
            </span>
          </div>
        )}

        {/* CTA buttons */}
        <div className="flex gap-3">
          <button
            onClick={onViewPortfolio}
            className="flex items-center gap-2 px-5 py-3 rounded-xl text-sm font-medium text-white transition-opacity hover:opacity-90"
            style={{ backgroundColor: 'var(--ej-blue)' }}
          >
            <span>📊</span>
            View Portfolio Analysis
          </button>

          <button
            onClick={onViewAssistant}
            className="flex items-center gap-2 px-5 py-3 rounded-xl text-sm font-medium border border-gray-200 bg-white text-gray-700 hover:bg-gray-50 transition-colors"
          >
            <span>🤖</span>
            AI Assistant
          </button>
        </div>

        {/* Subtle footer note */}
        <p className="text-xs text-gray-300 mt-8 text-center">
          Portfolio data sourced from PostgreSQL · Alerts sourced from Databricks SQL Warehouse
        </p>

      </div>
    </div>
  )
}
