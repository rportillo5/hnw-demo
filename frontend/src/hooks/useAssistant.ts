import { useState } from 'react'

interface WhatIfResult {
  realizedGainLoss: number
  estimatedTax: number
  afterTaxProceeds: number
  washSaleWarning: boolean
  washSaleExplanation: string | null
}

interface AssistantResponse {
  intent: string
  explanation?: string
  disclaimer?: string
  whatIfResult?: WhatIfResult
  message?: string
}

interface AlertContext {
  alertId: string
  alertType: string
  ticker: string | null
  detail: string
}

interface UseAssistantReturn {
  response: AssistantResponse | null
  loading: boolean
  error: string | null
  query: (
    text: string,
    lotId: string | null,
    alertContext: AlertContext | null,
    chunkCallback?: (chunk: string) => void
  ) => Promise<void>
  clear: () => void
}

const PORTFOLIO_ID = '10000000-0000-0000-0000-000000000001'

export function useAssistant(): UseAssistantReturn {
  const [response, setResponse] = useState<AssistantResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const query = async (
    text: string,
    lotId: string | null,
    alertContext: AlertContext | null,
    chunkCallback?: (chunk: string) => void
  ) => {
    setLoading(true)
    setError(null)
    setResponse(null)

    const body = {
      portfolioId: PORTFOLIO_ID,
      currentLotId: lotId ?? null,
      text,
      alertContext: alertContext ?? null,
    }

    try {
      const res = await fetch('/api/assistant/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      })

      if (!res.ok || !res.body) {
        throw new Error(`Request failed: ${res.status}`)
      }

      const reader = res.body.getReader()
      const decoder = new TextDecoder()

      let intent = ''
      let accumulatedText = ''
      let whatIfResult: WhatIfResult | undefined
      let eventName = ''
      let lineBuffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        lineBuffer += decoder.decode(value, { stream: true })
        const lines = lineBuffer.split('\n')
        lineBuffer = lines.pop() ?? ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
            continue
          }

          if (!line.startsWith('data:')) continue

          let data: any
          try {
            data = JSON.parse(line.slice(5))
          } catch {
            eventName = ''
            continue
          }

          if (eventName === 'intent') {
            intent = data.intent

          } else if (eventName === 'chunk') {
            accumulatedText += data.text
            // Write directly to DOM via callback — bypasses React's scheduler
            // entirely so every chunk paints immediately.
            chunkCallback?.(data.text)

          } else if (eventName === 'whatif') {
            whatIfResult = data

          } else if (eventName === 'done') {
            const finalResponse: AssistantResponse = {
              intent,
              explanation: data.disclaimer ? accumulatedText : undefined,
              disclaimer: data.disclaimer || undefined,
              whatIfResult,
              message: data.message || undefined,
            }
            setResponse(finalResponse)
            setLoading(false)

          } else if (eventName === 'error') {
            throw new Error(data.message)
          }

          eventName = ''
        }
      }
    } catch (err) {
      console.error(err)
      setError('Failed to get AI response — is the backend running?')
      setLoading(false)
    }
  }

  const clear = () => {
    setResponse(null)
    setError(null)
  }

  return { response, loading, error, query, clear }
}
