interface DatabricksIconProps {
  size?: number
}

/**
 * Databricks geometric logo mark (the brickwork/spark icon).
 * Used in AlertCard badge and TopBar connection status indicator.
 */
export default function DatabricksIcon({ size = 16 }: DatabricksIconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="white"
      xmlns="http://www.w3.org/2000/svg"
      aria-label="Databricks"
    >
      <path d="M12 1.5L2 7.25v9.5L12 22.5l10-5.75v-9.5L12 1.5zm0 2.31l7.5 4.33v7.72L12 20.19l-7.5-4.33V8.14L12 3.81z" />
      <path d="M12 6.5L6 10v8l6 3.5 6-3.5v-8L12 6.5zm0 2.31l3.5 2.02v4.34L12 17.19l-3.5-2.02v-4.34L12 8.81z" />
    </svg>
  )
}
