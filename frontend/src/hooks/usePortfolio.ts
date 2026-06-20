import { useEffect, useState } from 'react'
import { HarvestOpportunity, DriftResult } from '../types/api'

const PORTFOLIO_ID = '10000000-0000-0000-0000-000000000001'

interface UsePortfolioReturn {
  opportunities: HarvestOpportunity[]
  drift: DriftResult[]
  loading: boolean
  error: string | null
  refresh: () => void
}

export function usePortfolio(): UsePortfolioReturn {
  const [opportunities, setOpportunities] = useState<HarvestOpportunity[]>([])
  const [drift, setDrift] = useState<DriftResult[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [tick, setTick] = useState(0)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    Promise.all([
      fetch(`/api/portfolios/${PORTFOLIO_ID}/harvest-opportunities`).then(r => r.json()),
      fetch(`/api/portfolios/${PORTFOLIO_ID}/drift`).then(r => r.json()),
    ])
      .then(([opps, driftData]) => {
        if (!cancelled) {
          setOpportunities(opps)
          setDrift(driftData)
          setLoading(false)
        }
      })
      .catch(err => {
        if (!cancelled) {
          setError('Failed to load portfolio data — is the backend running?')
          setLoading(false)
          console.error(err)
        }
      })

    return () => { cancelled = true }
  }, [tick])

  const refresh = () => setTick(t => t + 1)

  return { opportunities, drift, loading, error, refresh }
}
