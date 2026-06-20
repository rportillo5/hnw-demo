import { HnwAlert } from '../types/api'
import DatabricksIcon from './DatabricksIcon'

interface AlertCardProps {
  alert: HnwAlert
  onDismiss: (alertId: string) => void
  onAskAi: (alert: HnwAlert) => void
}

const SEVERITY_BORDER: Record<HnwAlert['severity'], string> = {
  HIGH:   'border-l-4 border-red-600',
  MEDIUM: 'border-l-4 border-amber-500',
  LOW:    'border-l-4 border-blue-500',
}

const SEVERITY_LABEL: Record<HnwAlert['severity'], string> = {
  HIGH:   'text-red-600',
  MEDIUM: 'text-amber-600',
  LOW:    'text-blue-500',
}

const AI_ELIGIBLE_TYPES: HnwAlert['alertType'][] = [
  'CONCENTRATION',
  'HARVEST_OPPORTUNITY',
]

export default function AlertCard({ alert, onDismiss, onAskAi }: AlertCardProps) {
  const showAskAi = AI_ELIGIBLE_TYPES.includes(alert.alertType)

  return (
    <div
      className={`
        alert-slide-up bg-white rounded-lg shadow-md p-4 min-w-72 max-w-sm
        ${SEVERITY_BORDER[alert.severity]}
      `}
    >
      {/* Header row: severity + Databricks badge */}
      <div className="flex items-start justify-between mb-2">
        <span className={`text-xs font-semibold uppercase tracking-wide ${SEVERITY_LABEL[alert.severity]}`}>
          {alert.severity}
        </span>
        {/* Databricks source badge */}
        <span
          className="flex items-center gap-1 text-white text-xs font-medium px-2 py-0.5 rounded"
          style={{ backgroundColor: 'var(--databricks-red)' }}
        >
          <DatabricksIcon size={10} />
          Databricks
        </span>
      </div>

      {/* Alert title */}
      <p className="text-sm font-semibold text-gray-900 mb-1 leading-snug">
        {alert.title}
      </p>

      {/* Alert detail — max 2 lines */}
      <p className="text-xs text-gray-500 mb-3 line-clamp-2 leading-relaxed">
        {alert.detail}
      </p>

      {/* Action buttons */}
      <div className="flex items-center gap-2">
        {showAskAi && (
          <button
            onClick={() => onAskAi(alert)}
            className="text-xs font-medium px-3 py-1.5 rounded text-white transition-opacity hover:opacity-90"
            style={{ backgroundColor: 'var(--ej-blue)' }}
          >
            Ask AI
          </button>
        )}
        <button
          onClick={() => onDismiss(alert.alertId)}
          className="text-xs font-medium px-3 py-1.5 rounded border border-gray-300 text-gray-600 hover:bg-gray-50 transition-colors"
        >
          Dismiss
        </button>
        {alert.ticker && (
          <span className="ml-auto text-xs font-mono bg-gray-100 text-gray-600 px-2 py-1 rounded">
            {alert.ticker}
          </span>
        )}
      </div>
    </div>
  )
}
