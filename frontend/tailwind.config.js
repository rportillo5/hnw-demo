/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        'ej-blue':  '#003087',
        'ej-gold':  '#FFCC00',
        'db-red':   '#FF3621',   // Databricks brand red
      }
    }
  },
  plugins: []
}
