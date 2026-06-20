import { useState } from 'react'

interface AssistantQueryBoxProps {
  onSubmit: (text: string) => void
  loading: boolean
  placeholder?: string
}

const QUICK_QUERIES = [
  'Explain this lot',
  'What if I sell this and buy VTI?',
  'Summarize the portfolio',
  'Should I harvest this loss?',
]

export default function AssistantQueryBox({
  onSubmit,
  loading,
  placeholder = 'Ask about this lot or portfolio...',
}: AssistantQueryBoxProps) {
  const [text, setText] = useState('')

  const handleSubmit = () => {
    const trimmed = text.trim()
    if (!trimmed || loading) return
    onSubmit(trimmed)
    setText('')
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit()
    }
  }

  return (
    <div className="bg-white rounded-lg shadow-sm p-4 mb-4">
      <div className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
        Ask the AI Assistant
      </div>

      {/* Quick query chips */}
      <div className="flex flex-wrap gap-2 mb-3">
        {QUICK_QUERIES.map(q => (
          <button
            key={q}
            onClick={() => onSubmit(q)}
            disabled={loading}
            className="text-xs px-3 py-1.5 rounded-full border border-gray-200 text-gray-600 hover:border-blue-300 hover:text-blue-700 hover:bg-blue-50 transition-colors disabled:opacity-50"
          >
            {q}
          </button>
        ))}
      </div>

      {/* Free-text input */}
      <div className="flex gap-2">
        <input
          type="text"
          value={text}
          onChange={e => setText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={loading}
          className="flex-1 text-sm border border-gray-200 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-200 disabled:opacity-50"
        />
        <button
          onClick={handleSubmit}
          disabled={!text.trim() || loading}
          className="text-sm font-medium px-4 py-2 rounded text-white transition-opacity hover:opacity-90 disabled:opacity-40"
          style={{ backgroundColor: 'var(--ej-blue)' }}
        >
          {loading ? '...' : 'Ask'}
        </button>
      </div>
    </div>
  )
}
