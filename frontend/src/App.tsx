import { useState, useRef } from 'react'
import { HnwAlert } from './types/api'
import { useAlerts } from './hooks/useAlerts'
import { usePortfolio } from './hooks/usePortfolio'
import { useAssistant } from './hooks/useAssistant'
import TopBar from './components/TopBar'
import Sidebar from './components/Sidebar'
import AlertTray from './components/AlertTray'
import ClientSplashPage from './components/ClientSplashPage'
import SummaryPanel from './components/SummaryPanel'
import HarvestOpportunitiesList from './components/HarvestOpportunitiesList'
import AllocationDriftPanel from './components/AllocationDriftPanel'
import AssistantQueryBox from './components/AssistantQueryBox'
import AiExplanationPanel from './components/AiExplanationPanel'
import WhatIfPanel from './components/WhatIfPanel'

export const PORTFOLIO_ID = '10000000-0000-0000-0000-000000000001'

export default function App() {
  // 'splash' is the default landing section
  const [activeSection, setActiveSection] = useState('splash')
  const [selectedLotId, setSelectedLotId] = useState<string | null>(null)
  const [pendingAiAlert, setPendingAiAlert] = useState<HnwAlert | null>(null)

  const { alerts, sseConnected, dismissAlert } = useAlerts()
  const { opportunities, drift, loading, error } = usePortfolio()
  const assistant = useAssistant()

  // Ref pointing at the <span> inside AiExplanationPanel that receives raw
  // streaming chunks. Writing textContent directly bypasses React's scheduler
  // so every token paints immediately — no batching, no deferred commits.
  const streamingSpanRef = useRef<HTMLSpanElement>(null)

  const chunkCallback = (chunk: string) => {
    if (streamingSpanRef.current) {
      streamingSpanRef.current.textContent =
        (streamingSpanRef.current.textContent ?? '') + chunk
    }
  }

  const handleSelectLot = (lotId: string) => {
    setSelectedLotId(lotId)
    setPendingAiAlert(null)
    assistant.clear()
    setActiveSection('assistant')
  }

  const handleAskAi = (alert: HnwAlert) => {
    setPendingAiAlert(alert)
    setSelectedLotId(null)
    assistant.clear()
    setActiveSection('assistant')
    if (streamingSpanRef.current) streamingSpanRef.current.textContent = ''

    assistant.query(
      `explain the ${alert.ticker ? alert.ticker + ' ' : ''}${alert.alertType.toLowerCase().replace('_', ' ')} alert`,
      null,
      {
        alertId: alert.alertId,
        alertType: alert.alertType,
        ticker: alert.ticker,
        detail: alert.detail,
      },
      chunkCallback
    )
  }

  const handleAssistantQuery = (text: string) => {
    const alertContext = pendingAiAlert
      ? {
          alertId: pendingAiAlert.alertId,
          alertType: pendingAiAlert.alertType,
          ticker: pendingAiAlert.ticker,
          detail: pendingAiAlert.detail,
        }
      : null
    if (streamingSpanRef.current) streamingSpanRef.current.textContent = ''
    assistant.query(text, selectedLotId, alertContext, chunkCallback)
  }

  const renderMain = () => {
    switch (activeSection) {

      case 'splash':
        return (
          <ClientSplashPage
            opportunities={opportunities}
            drift={drift}
            loading={loading}
            alertCount={alerts.length}
            onViewPortfolio={() => setActiveSection('summary')}
            onViewAssistant={() => setActiveSection('assistant')}
          />
        )

      case 'summary':
        return (
          <SummaryPanel
            opportunities={opportunities}
            drift={drift}
            loading={loading}
            error={error}
          />
        )

      case 'harvest':
        return (
          <div className="p-6">
            <h2 className="text-base font-semibold text-gray-800 mb-5">
              Tax-Loss Harvest Opportunities
            </h2>
            <HarvestOpportunitiesList
              opportunities={opportunities}
              loading={loading}
              selectedLotId={selectedLotId}
              onSelectLot={handleSelectLot}
            />
          </div>
        )

      case 'drift':
        return (
          <div className="p-6">
            <h2 className="text-base font-semibold text-gray-800 mb-5">
              Allocation Drift
            </h2>
            <AllocationDriftPanel drift={drift} loading={loading} />
          </div>
        )

      case 'assistant':
        return (
          <div className="p-6">
            <h2 className="text-base font-semibold text-gray-800 mb-5">
              AI Assistant
            </h2>

            {pendingAiAlert && (
              <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg text-sm flex items-start justify-between">
                <div>
                  <span className="font-semibold text-blue-800">
                    Databricks alert context:{' '}
                  </span>
                  <span className="text-blue-700">{pendingAiAlert.title}</span>
                </div>
                <button
                  className="text-xs text-blue-500 underline ml-4 flex-shrink-0"
                  onClick={() => { setPendingAiAlert(null); assistant.clear() }}
                >
                  clear
                </button>
              </div>
            )}

            {selectedLotId && !pendingAiAlert && (
              <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-sm flex items-start justify-between">
                <div>
                  {(() => {
                    const opp = opportunities.find(o => o.lotId === selectedLotId)
                    return opp
                      ? <span className="font-semibold text-green-800">
                          {opp.ticker} {opp.lotLabel} — unrealized loss:{' '}
                          <span className="text-red-600">
                            (${Math.abs(opp.unrealizedLoss).toLocaleString()})
                          </span>
                        </span>
                      : <span className="text-green-700 font-mono text-xs">{selectedLotId}</span>
                  })()}
                </div>
                <button
                  className="text-xs text-green-500 underline ml-4 flex-shrink-0"
                  onClick={() => { setSelectedLotId(null); assistant.clear() }}
                >
                  clear
                </button>
              </div>
            )}

            <AssistantQueryBox
              onSubmit={handleAssistantQuery}
              loading={assistant.loading}
              hasLotContext={!!selectedLotId}
              hasAlertContext={!!pendingAiAlert}
              placeholder={
                pendingAiAlert
                  ? `Ask about the ${pendingAiAlert.ticker ?? ''} alert...`
                  : selectedLotId
                  ? 'Ask about this lot (e.g. "what if I sell and buy VTI?")'
                  : 'Ask about the portfolio...'
              }
            />

            {assistant.error && (
              <div className="bg-red-50 border border-red-200 rounded p-3 text-sm text-red-700 mb-4">
                {assistant.error}
              </div>
            )}

            {assistant.response?.whatIfResult && (
              <WhatIfPanel result={assistant.response.whatIfResult} />
            )}

            {(assistant.loading || assistant.response?.explanation) && (
              <AiExplanationPanel
                explanation={assistant.response?.explanation ?? ''}
                disclaimer={assistant.response?.disclaimer ?? ''}
                loading={assistant.loading}
                streamingRef={streamingSpanRef}
              />
            )}

            {assistant.response?.intent === 'UNSUPPORTED' && (
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 text-sm text-gray-600">
                {assistant.response.message}
              </div>
            )}
          </div>
        )

      default:
        return null
    }
  }

  return (
    <div className="flex flex-col min-h-screen">
      <TopBar
        databricksConnected={true}
        sseConnected={sseConnected}
      />

      <div className="flex flex-1 overflow-hidden">
        <Sidebar
          activeSection={activeSection}
          onNavigate={setActiveSection}
          alertCount={alerts.length}
        />

        <main
          className="flex-1 overflow-y-auto pb-40"
          style={{ background: 'var(--surface)' }}
        >
          {renderMain()}
        </main>
      </div>

      <AlertTray
        alerts={alerts}
        onDismiss={dismissAlert}
        onAskAi={handleAskAi}
      />
    </div>
  )
}
