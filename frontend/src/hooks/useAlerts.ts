import { useEffect, useRef, useState } from 'react'
import { HnwAlert } from '../types/api'

interface UseAlertsReturn {
  alerts: HnwAlert[]
  sseConnected: boolean
  dismissAlert: (alertId: string) => Promise<void>
  clearAll: () => void
}

/**
 * Manages the SSE connection to /api/alerts/stream and the resulting
 * alert state. Handles dismiss (PATCH /api/alerts/{id}/read) and
 * exposes SSE connection status for the TopBar indicator.
 */
export function useAlerts(): UseAlertsReturn {
  const [alerts, setAlerts] = useState<HnwAlert[]>([])
  const [sseConnected, setSseConnected] = useState(false)
  const clientIdRef = useRef(crypto.randomUUID())

  useEffect(() => {
    const clientId = clientIdRef.current
    const es = new EventSource(`/api/alerts/stream?clientId=${clientId}`)

    es.onopen = () => {
      setSseConnected(true)
      console.log('SSE connected, clientId:', clientId)
    }

    es.addEventListener('alert', (event: MessageEvent) => {
      const alert: HnwAlert = JSON.parse(event.data)
      console.log('Alert received:', alert.title)
      setAlerts(prev => [alert, ...prev])
    })

    es.onerror = () => {
      setSseConnected(false)
      console.warn('SSE connection error — will retry automatically')
    }

    return () => {
      es.close()
      setSseConnected(false)
    }
  }, [])

  const dismissAlert = async (alertId: string) => {
    try {
      await fetch(`/api/alerts/${alertId}/read`, { method: 'PATCH' })
      setAlerts(prev => prev.filter(a => a.alertId !== alertId))
    } catch (err) {
      console.error('Failed to dismiss alert:', err)
    }
  }

  const clearAll = () => setAlerts([])

  return { alerts, sseConnected, dismissAlert, clearAll }
}
