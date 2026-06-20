import { RefObject } from 'react'

interface AiExplanationPanelProps {
  explanation: string
  disclaimer: string
  loading: boolean
  streamingRef?: RefObject<HTMLSpanElement>
}

/**
 * Renders bold markdown (**text**) as <strong> tags and
 * highlights the "You might tell your client:" talking point
 * in a distinct callout box.
 */
function renderWithHighlights(text: string) {
  return text.split(/\*\*(.*?)\*\*/g).map((part, i) =>
    i % 2 === 1
      ? <strong key={i} className="font-semibold text-gray-900">{part}</strong>
      : <span key={i}>{part}</span>
  )
}

function splitExplanationAndTalkingPoint(text: string): {
  body: string
  talkingPoint: string | null
} {
  const marker = 'You might tell your client:'
  const idx = text.indexOf(marker)
  if (idx === -1) return { body: text, talkingPoint: null }

  return {
    body: text.slice(0, idx).trim(),
    talkingPoint: text.slice(idx + marker.length).trim().replace(/^["']|["']$/g, ''),
  }
}

export default function AiExplanationPanel({
  explanation,
  disclaimer,
  loading,
  streamingRef,
}: AiExplanationPanelProps) {
  // While the LLM is streaming, show a live text panel. Chunks are written
  // directly to streamingRef.current.textContent by App.tsx's chunkCallback,
  // bypassing React's scheduler entirely — every token paints immediately.
  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-5 mb-4">
        <div
          className="text-xs font-semibold uppercase tracking-wide mb-3"
          style={{ color: 'var(--ej-blue)' }}
        >
          AI Explanation
        </div>
        <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap font-mono">
          <span ref={streamingRef} />
          <span className="animate-pulse ml-0.5 text-blue-600">▋</span>
        </p>
      </div>
    )
  }

  if (!explanation) return null

  // Stream finished — render the fully formatted view.
  const withoutDisclaimer = explanation.replace(disclaimer, '').trim()
  const { body, talkingPoint } = splitExplanationAndTalkingPoint(withoutDisclaimer)

  return (
    <div className="bg-white rounded-lg shadow-sm p-5 mb-4">
      <div
        className="text-xs font-semibold uppercase tracking-wide mb-3"
        style={{ color: 'var(--ej-blue)' }}
      >
        AI Explanation
      </div>

      {/* Main explanation body with bold highlights */}
      <p className="text-sm text-gray-700 leading-relaxed mb-4">
        {renderWithHighlights(body)}
      </p>

      {/* Advisor talking point callout */}
      {talkingPoint && (
        <div
          className="rounded-lg p-4 mb-4 border-l-4"
          style={{
            backgroundColor: '#EFF6FF',
            borderLeftColor: 'var(--ej-blue)',
          }}
        >
          <div className="text-xs font-semibold uppercase tracking-wide mb-1"
            style={{ color: 'var(--ej-blue)' }}>
            💬 You might tell your client:
          </div>
          <p className="text-sm text-blue-900 leading-relaxed italic">
            "{talkingPoint}"
          </p>
        </div>
      )}

      {/* Disclaimer */}
      <div className="bg-amber-50 border border-amber-200 rounded p-3 text-xs text-amber-800 leading-relaxed">
        ⚠️ {disclaimer}
      </div>
    </div>
  )
}
