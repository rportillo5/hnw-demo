import MetricCards from './MetricCards'
import AllocationDriftPanel from './AllocationDriftPanel'
import { HarvestOpportunity, DriftResult } from '../types/api'

interface SummaryPanelProps {
  opportunities: HarvestOpportunity[]
  drift: DriftResult[]
  loading: boolean
  error: string | null
}

export default function SummaryPanel({
  opportunities, drift, loading, error
}: SummaryPanelProps) {
  if (error) {
    return (
      <div className="p-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">
          {error}
        </div>
      </div>
    )
  }

  return (
    <div className="p-6">
      <h2 className="text-base font-semibold text-gray-800 mb-5">
        Portfolio Overview — Margaret Holloway
      </h2>
      <MetricCards opportunities={opportunities} drift={drift} loading={loading} />
      <AllocationDriftPanel drift={drift} loading={loading} />
    </div>
  )
}
