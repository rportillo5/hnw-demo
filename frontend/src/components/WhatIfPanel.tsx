interface WhatIfResult {
  realizedGainLoss: number
  estimatedTax: number
  afterTaxProceeds: number
  washSaleWarning: boolean
  washSaleExplanation: string | null
}

interface WhatIfPanelProps {
  result: WhatIfResult
}

function fmtDollar(n: number) {
  const abs = Math.abs(n).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
  return n < 0 ? `($${abs})` : `$${abs}`
}

export default function WhatIfPanel({ result }: WhatIfPanelProps) {
  const taxIsSavings = result.estimatedTax < 0

  return (
    <div className="bg-white rounded-lg shadow-sm p-5 mb-4">
      <div className="text-xs font-semibold uppercase tracking-wide mb-4"
        style={{ color: 'var(--ej-blue)' }}>
        What-If Results
      </div>

      {/* Wash-sale warning banner */}
      {result.washSaleWarning && (
        <div className="bg-red-50 border border-red-300 rounded p-3 mb-4 text-xs text-red-800">
          ⚠️ <strong>Wash-Sale Warning</strong><br />
          {result.washSaleExplanation}
        </div>
      )}

      {/* Result metrics */}
      <div className="grid grid-cols-3 gap-4">
        <div className="text-center p-3 rounded-lg bg-gray-50">
          <div className="text-xs text-gray-500 mb-1">Realized Gain/Loss</div>
          <div className={`text-lg font-bold ${result.realizedGainLoss < 0 ? 'text-red-600' : 'text-green-600'}`}>
            {fmtDollar(result.realizedGainLoss)}
          </div>
        </div>

        <div className="text-center p-3 rounded-lg bg-gray-50">
          <div className="text-xs text-gray-500 mb-1">
            {taxIsSavings ? 'Est. Tax Savings' : 'Est. Tax Owed'}
          </div>
          <div className={`text-lg font-bold ${taxIsSavings ? 'text-green-600' : 'text-amber-600'}`}>
            {taxIsSavings
              ? `$${Math.abs(result.estimatedTax).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
              : fmtDollar(result.estimatedTax)
            }
          </div>
        </div>

        <div className="text-center p-3 rounded-lg bg-gray-50">
          <div className="text-xs text-gray-500 mb-1">After-Tax Proceeds</div>
          <div className="text-lg font-bold text-gray-800">
            {fmtDollar(result.afterTaxProceeds)}
          </div>
        </div>
      </div>
    </div>
  )
}
