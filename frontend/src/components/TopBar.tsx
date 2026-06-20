import DatabricksIcon from './DatabricksIcon'

interface TopBarProps {
  databricksConnected: boolean
  sseConnected: boolean
}

export default function TopBar({ databricksConnected, sseConnected }: TopBarProps) {
  const statusColor = databricksConnected && sseConnected
    ? 'bg-green-500'
    : 'bg-gray-400'

  const statusLabel = databricksConnected && sseConnected
    ? 'Databricks Live'
    : sseConnected
    ? 'Databricks Connecting...'
    : 'Databricks Offline'

  return (
    <header
      className="flex items-center justify-between px-6 py-3 shadow-sm"
      style={{ backgroundColor: 'var(--ej-blue)' }}
    >
      {/* Left: branding */}
      <div className="flex items-center gap-3">
        <div
          className="w-8 h-8 rounded flex items-center justify-center font-bold text-sm"
          style={{ backgroundColor: 'var(--ej-gold)', color: 'var(--ej-blue)' }}
        >
          EJ
        </div>
        <div>
          <div className="text-white font-semibold text-sm">
            EJ Advisor Dashboard
          </div>
          <div className="text-blue-200 text-xs">
            Margaret Holloway · HNW · $4,250,000 AUM
          </div>
        </div>
      </div>

      {/* Right: Databricks status badge */}
      <div className="flex items-center gap-2 bg-white/10 rounded-full px-3 py-1.5">
        <DatabricksIcon size={14} />
        <span className="text-white text-xs font-medium">
          {statusLabel}
        </span>
        <span className={`w-2 h-2 rounded-full ${statusColor} ${databricksConnected && sseConnected ? 'animate-pulse' : ''}`} />
      </div>
    </header>
  )
}
