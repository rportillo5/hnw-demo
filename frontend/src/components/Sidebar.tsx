interface SidebarProps {
  activeSection: string
  onNavigate: (section: string) => void
  alertCount?: number
}

const NAV_ITEMS = [
  { id: 'splash',    label: 'Client Overview', icon: '👤' },
  { id: 'summary',   label: 'Summary',          icon: '📊' },
  { id: 'harvest',   label: 'Tax Harvest',       icon: '🌿' },
  { id: 'drift',     label: 'Drift',             icon: '📉' },
  { id: 'assistant', label: 'AI Assistant',      icon: '🤖' },
]

export default function Sidebar({ activeSection, onNavigate, alertCount = 0 }: SidebarProps) {
  return (
    <nav className="w-48 min-h-screen bg-white border-r border-gray-200 pt-4 flex-shrink-0">
      {NAV_ITEMS.map(item => (
        <button
          key={item.id}
          onClick={() => onNavigate(item.id)}
          className={`
            w-full flex items-center gap-3 px-4 py-3 text-sm text-left
            transition-colors hover:bg-blue-50
            ${activeSection === item.id
              ? 'font-semibold border-r-2'
              : 'text-gray-600'
            }
          `}
          style={activeSection === item.id
            ? { borderRightColor: 'var(--ej-blue)', color: 'var(--ej-blue)' }
            : {}
          }
        >
          <span>{item.icon}</span>
          <span className="flex-1">{item.label}</span>

          {/* Alert badge on Client Overview */}
          {item.id === 'splash' && alertCount > 0 && (
            <span className="text-xs font-bold text-white rounded-full w-4 h-4 flex items-center justify-center"
              style={{ backgroundColor: '#FF3621', fontSize: '10px' }}>
              {alertCount > 9 ? '9+' : alertCount}
            </span>
          )}
        </button>
      ))}
    </nav>
  )
}
