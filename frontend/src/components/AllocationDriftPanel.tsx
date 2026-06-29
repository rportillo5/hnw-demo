import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ReferenceLine, ResponsiveContainer, Cell
} from 'recharts'
import { DriftResult } from '../types/api'

interface AllocationDriftPanelProps {
  drift: DriftResult[]
  loading: boolean
}

/** A holding breaches the 5/25 Rule if it drifts ≥5 pts OR ≥25% relative from target. */
function breaches525(driftPct: number, targetPct: number): boolean {
  if (targetPct === 0) return false
  return Math.abs(driftPct) >= 5 || Math.abs(driftPct / targetPct) >= 0.25
}

export default function AllocationDriftPanel({ drift, loading }: AllocationDriftPanelProps) {
  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-5 mb-6">
        <h3 className="text-sm font-semibold text-gray-700 mb-4">Allocation Drift</h3>
        <div className="skeleton h-48 w-full" />
      </div>
    )
  }

  // Sort by drift magnitude (largest absolute drift first)
  const sorted = [...drift].sort((a, b) => Math.abs(b.driftPct) - Math.abs(a.driftPct))

  const data = sorted.map(d => ({
    ticker: d.ticker,
    drift: parseFloat(d.driftPct.toFixed(2)),
    current: parseFloat(d.currentPct.toFixed(2)),
    target: parseFloat(d.targetPct.toFixed(2)),
    breaches: breaches525(d.driftPct, d.targetPct),
  }))

  const breachCount = data.filter(d => d.breaches).length

  return (
    <div className="space-y-4">

      {/* 5/25 Rule explainer */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="text-xs font-semibold uppercase tracking-wide mb-1"
          style={{ color: 'var(--ej-blue)' }}>
          The 5/25 Rebalancing Rule
        </div>
        <p className="text-sm text-blue-900 leading-relaxed">
          A holding triggers a rebalancing review when it drifts more than{' '}
          <strong>5 percentage points</strong> from its target allocation,{' '}
          <em>or</em> more than <strong>25% relative</strong> to that target —
          whichever threshold is crossed first. For example, a holding targeted
          at 8% that falls to 5% has drifted −3 pts (below the 5-pt threshold)
          but 37.5% relative (above the 25% threshold), so it still triggers a review.
        </p>
      </div>

      {/* Attention badge */}
      {breachCount > 0 ? (
        <div className="flex items-center gap-2 px-4 py-2 bg-amber-50 border border-amber-300 rounded-lg text-sm font-medium text-amber-800">
          ⚠️ {breachCount} holding{breachCount > 1 ? 's' : ''} require{breachCount === 1 ? 's' : ''} rebalancing attention
        </div>
      ) : (
        <div className="flex items-center gap-2 px-4 py-2 bg-green-50 border border-green-200 rounded-lg text-sm font-medium text-green-800">
          ✅ All holdings are within the 5/25 thresholds
        </div>
      )}

      {/* Chart */}
      <div className="bg-white rounded-lg shadow-sm p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-gray-700">Allocation Drift vs Target</h3>
          <div className="flex items-center gap-4 text-xs text-gray-500">
            <span className="flex items-center gap-1">
              <span className="w-3 h-3 rounded-sm inline-block" style={{ backgroundColor: '#DC2626' }} />
              Over-weight
            </span>
            <span className="flex items-center gap-1">
              <span className="w-3 h-3 rounded-sm inline-block" style={{ backgroundColor: '#003087' }} />
              Under-weight
            </span>
            <span className="flex items-center gap-1">
              <span className="w-3 h-1 inline-block border-t-2 border-dashed border-amber-500" />
              5/25 threshold
            </span>
          </div>
        </div>

        <ResponsiveContainer width="100%" height={280}>
          <BarChart
            data={data}
            layout="vertical"
            margin={{ top: 0, right: 40, left: 10, bottom: 0 }}
          >
            <CartesianGrid strokeDashboard="3 3" horizontal={false} stroke="#f0f0f0" />
            <XAxis
              type="number"
              domain={['dataMin - 1', 'dataMax + 1']}
              tickFormatter={v => `${v}%`}
              tick={{ fontSize: 11 }}
            />
            <YAxis
              type="category"
              dataKey="ticker"
              width={42}
              tick={{ fontSize: 12, fontWeight: 600 }}
            />
            <Tooltip
              formatter={(value: number, _name: string, props: { payload?: { current: number; target: number; breaches: boolean } }) => {
                const payload = props.payload
                const flag = payload?.breaches ? ' ⚠️ exceeds 5/25 threshold' : ''
                return [
                  `Drift: ${value > 0 ? '+' : ''}${value}% (current: ${payload?.current}%, target: ${payload?.target}%)${flag}`,
                  ''
                ]
              }}
              contentStyle={{ fontSize: 12 }}
            />
            {/* Zero line */}
            <ReferenceLine x={0} stroke="#9CA3AF" strokeWidth={1.5} />
            {/* 5/25 threshold lines */}
            <ReferenceLine x={5}  stroke="#F59E0B" strokeWidth={1.5} strokeDasharray="4 3" />
            <ReferenceLine x={-5} stroke="#F59E0B" strokeWidth={1.5} strokeDasharray="4 3" />
            <Bar dataKey="drift" radius={[0, 3, 3, 0]}>
              {data.map((entry, index) => (
                <Cell
                  key={`cell-${index}`}
                  fill={entry.drift > 0 ? '#DC2626' : '#003087'}
                  opacity={entry.breaches ? 1 : 0.55}
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
