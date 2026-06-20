import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ReferenceLine, ResponsiveContainer, Cell
} from 'recharts'
import { DriftResult } from '../types/api'

interface AllocationDriftPanelProps {
  drift: DriftResult[]
  loading: boolean
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
  }))

  return (
    <div className="bg-white rounded-lg shadow-sm p-5 mb-6">
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
        </div>
      </div>

      <ResponsiveContainer width="100%" height={280}>
        <BarChart
          data={data}
          layout="vertical"
          margin={{ top: 0, right: 40, left: 10, bottom: 0 }}
        >
          <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f0f0f0" />
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
            formatter={(value: number, _name: string, props: { payload?: { current: number; target: number } }) => {
              const payload = props.payload
              return [
                `Drift: ${value > 0 ? '+' : ''}${value}% (current: ${payload?.current}%, target: ${payload?.target}%)`,
                ''
              ]
            }}
            contentStyle={{ fontSize: 12 }}
          />
          <ReferenceLine x={0} stroke="#9CA3AF" strokeWidth={1.5} />
          <Bar dataKey="drift" radius={[0, 3, 3, 0]}>
            {data.map((entry, index) => (
              <Cell
                key={`cell-${index}`}
                fill={entry.drift > 0 ? '#DC2626' : '#003087'}
                opacity={0.85}
              />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
