import { useState } from 'react'
import { HnwAlert } from '../types/api'
import AlertCard from './AlertCard'

interface AlertTrayProps {
  alerts: HnwAlert[]
  onDismiss: (alertId: string) => void
  onAskAi: (alert: HnwAlert) => void
}

export default function AlertTray({ alerts, onDismiss, onAskAi }: AlertTrayProps) {
  const [minimized, setMinimized] = useState(false)

  if (alerts.length === 0) return null

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50">
      {/* Tray header bar */}
      <div
        className="flex items-center justify-between px-4 py-2 cursor-pointer"
        style={{ backgroundColor: 'var(--ej-blue)' }}
        onClick={() => setMinimized(m => !m)}
      >
        <div className="flex items-center gap-2">
          <span className="text-white text-xs font-semibold">
            Databricks Alerts
          </span>
          <span className="bg-red-500 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
            {alerts.length}
          </span>
        </div>
        <span className="text-blue-200 text-xs">
          {minimized ? '▲ Show' : '▼ Minimize'}
        </span>
      </div>

      {/* Alert cards row */}
      {!minimized && (
        <div
          className="flex gap-3 p-3 overflow-x-auto"
          style={{ backgroundColor: '#F0F4FF' }}
        >
          {alerts.map(alert => (
            <AlertCard
              key={alert.alertId}
              alert={alert}
              onDismiss={onDismiss}
              onAskAi={onAskAi}
            />
          ))}
        </div>
      )}
    </div>
  )
}
