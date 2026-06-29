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

function fmtPct(n: number) {
  return (n * 100).toFixed(1) + '%'
}

export default function WhatIfPanel({ result }: WhatIfPanelProps) {
  const taxIsSavings = result.estimatedTax < 0
  const loss = Math.abs(result.realizedGainLoss)
  const savings = Math.abs(result.estimatedTax)
  const taxRate = loss > 0 ? savings / loss : 0

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
              ? `$${savings.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
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

      {/* Formula breakdown — shown only for harvested losses without a wash-sale violation */}
      {taxIsSavings && !result.washSaleWarning && (
        <div className="mt-4 bg-blue-50 border border-blue-100 rounded-lg p-3">
          <div className="text-xs font-semibold uppercase tracking-wide mb-2"
            style={{ color: 'var(--ej-blue)' }}>
            How this is calculated
          </div>
          <div className="text-xs text-gray-600 space-y-0.5 font-mono">
            <div>Realized loss&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;= ${loss.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
            <div>× Long-term cap. gains rate = {fmtPct(taxRate)}</div>
            <div className="border-t border-blue-200 pt-0.5 font-semibold text-green-700">
              = ${savings.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} estimated tax savings
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
