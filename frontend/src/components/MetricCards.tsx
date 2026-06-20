import { HarvestOpportunity, DriftResult } from '../types/api'

interface MetricCardsProps {
  opportunities: HarvestOpportunity[]
  drift: DriftResult[]
  loading: boolean
}

function fmt(n: number, prefix = '$') {
  return prefix + Math.abs(n).toLocaleString('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  })
}

export default function MetricCards({ opportunities, drift, loading }: MetricCardsProps) {
  const totalLoss = opportunities.reduce((sum, o) => sum + o.unrealizedLoss, 0)
  const totalSavings = opportunities.reduce((sum, o) => sum + o.estimatedTaxSavings, 0)
  const maxDrift = drift.length > 0
    ? Math.max(...drift.map(d => Math.abs(d.driftPct)))
    : 0

  const cards = [
    {
      label: 'AUM',
      value: '$4,250,000',
      sub: 'Margaret Holloway',
      color: 'var(--ej-blue)',
      textColor: 'white',
    },
    {
      label: 'Unrealized Losses',
      value: loading ? '—' : fmt(totalLoss),
      sub: `${opportunities.length} harvest candidates`,
      color: '#FEF2F2',
      textColor: '#DC2626',
    },
    {
      label: 'Est. Tax Savings',
      value: loading ? '—' : fmt(totalSavings),
      sub: 'if all losses harvested',
      color: '#F0FDF4',
      textColor: '#16A34A',
    },
    {
      label: 'Max Drift',
      value: loading ? '—' : `${maxDrift.toFixed(1)}%`,
      sub: 'from target allocation',
      color: '#FFFBEB',
      textColor: '#D97706',
    },
  ]

  return (
    <div className="grid grid-cols-4 gap-4 mb-6">
      {cards.map(card => (
        <div
          key={card.label}
          className="rounded-lg p-4 shadow-sm"
          style={{ backgroundColor: card.color }}
        >
          <div className="text-xs font-medium mb-1 opacity-75"
            style={{ color: card.textColor }}>
            {card.label}
          </div>
          <div className="text-2xl font-bold mb-0.5"
            style={{ color: card.textColor }}>
            {card.value}
          </div>
          <div className="text-xs opacity-60"
            style={{ color: card.textColor }}>
            {card.sub}
          </div>
        </div>
      ))}
    </div>
  )
}
