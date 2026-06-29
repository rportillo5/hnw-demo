import { HarvestOpportunity } from '../types/api'

interface HarvestOpportunitiesListProps {
  opportunities: HarvestOpportunity[]
  loading: boolean
  selectedLotId: string | null
  onSelectLot: (lotId: string) => void
}

function fmtDollar(n: number) {
  return '$' + Math.abs(n).toLocaleString('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  })
}

export default function HarvestOpportunitiesList({
  opportunities,
  loading,
  selectedLotId,
  onSelectLot,
}: HarvestOpportunitiesListProps) {

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-5">
        <h3 className="text-sm font-semibold text-gray-700 mb-4">
          Tax-Loss Harvest Opportunities
        </h3>
        {[1, 2, 3, 4].map(i => (
          <div key={i} className="skeleton h-10 w-full mb-2" />
        ))}
      </div>
    )
  }

  if (opportunities.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-5">
        <h3 className="text-sm font-semibold text-gray-700 mb-2">
          Tax-Loss Harvest Opportunities
        </h3>
        <p className="text-sm text-gray-400">No harvest candidates found.</p>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-lg shadow-sm p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-gray-700">
          Tax-Loss Harvest Opportunities
        </h3>
        <span className="text-xs text-gray-400">
          Click a row to explain with AI →
        </span>
      </div>

      {/* Tax-Loss Harvesting explainer */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
        <div className="text-xs font-semibold uppercase tracking-wide mb-1"
          style={{ color: 'var(--ej-blue)' }}>
          What Is Tax-Loss Harvesting?
        </div>
        <p className="text-sm text-blue-900 leading-relaxed">
          Tax-loss harvesting is the practice of selling a position that is currently
          sitting at an <strong>unrealized loss</strong> to lock in that loss for tax
          purposes. The realized loss can then offset capital gains elsewhere in the
          portfolio — reducing your tax bill. A <strong>replacement security</strong>{' '}
          is purchased immediately to keep the portfolio fully invested. Losses held
          longer than one year qualify as <strong>long-term</strong> (taxed at a lower
          rate); losses held one year or less are <strong>short-term</strong> (taxed as
          ordinary income, making them especially valuable to harvest). Be mindful of
          the <strong>wash-sale rule</strong>: repurchasing the same or a substantially
          identical security within 30 days before or after the sale disallows the loss.
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-100">
              <th className="text-left py-2 px-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                Ticker
              </th>
              <th className="text-left py-2 px-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                Lot
              </th>
              <th className="text-right py-2 px-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                Unrealized Loss
              </th>
              <th className="text-right py-2 px-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                Est. Tax Savings
              </th>
              <th className="text-right py-2 px-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                Days Held
              </th>
              <th className="text-center py-2 px-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                Type
              </th>
            </tr>
          </thead>
          <tbody>
            {opportunities.map(opp => {
              const isSelected = opp.lotId === selectedLotId
              return (
                <tr
                  key={opp.lotId}
                  onClick={() => onSelectLot(opp.lotId)}
                  className={`
                    border-b border-gray-50 cursor-pointer transition-colors
                    ${isSelected
                      ? 'bg-blue-50 border-l-2'
                      : 'hover:bg-gray-50'
                    }
                  `}
                  style={isSelected
                    ? { borderLeftColor: 'var(--ej-blue)' }
                    : {}
                  }
                >
                  <td className="py-3 px-3 font-semibold text-gray-900">
                    {opp.ticker}
                  </td>
                  <td className="py-3 px-3 text-gray-600">
                    {opp.lotLabel}
                  </td>
                  <td className="py-3 px-3 text-right font-medium"
                    style={{ color: 'var(--loss-red)' }}>
                    ({fmtDollar(opp.unrealizedLoss)})
                  </td>
                  <td className="py-3 px-3 text-right font-medium"
                    style={{ color: 'var(--gain-green)' }}>
                    {fmtDollar(opp.estimatedTaxSavings)}
                  </td>
                  <td className="py-3 px-3 text-right text-gray-600">
                    {opp.daysHeld}
                  </td>
                  <td className="py-3 px-3 text-center">
                    <span className={`
                      text-xs font-medium px-2 py-0.5 rounded-full
                      ${opp.period === 'LONG_TERM'
                        ? 'bg-green-100 text-green-700'
                        : 'bg-amber-100 text-amber-700'
                      }
                    `}>
                      {opp.period === 'LONG_TERM' ? 'Long-term' : 'Short-term'}
                    </span>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
