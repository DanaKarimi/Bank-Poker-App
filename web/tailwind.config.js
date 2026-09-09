/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'felt-green': '#0b4625',
        'felt-dark': '#062815',
        'felt-card': '#0f532d',
        'felt-card-dark': '#0a3a20',
        'gold-accent': '#d4af37',
        'gold-light': '#f3e5ab',
        'gold-dark': '#aa8c2c',
        'cream-text': '#f5f5dc',
        'cream-muted': 'rgba(245, 245, 220, 0.6)',
        'win-green': '#4caf50',
        'lose-red': '#f44336',
        'live-red': '#e53935',
        'silver': '#c0c0c0',
        'bronze': '#cd7f32',
      },
      borderRadius: {
        'sm': '8px',
        'md': '12px',
        'lg': '16px',
        'xl': '20px',
        '2xl': '24px',
        '3xl': '32px',
      }
    },
  },
  plugins: [],
}
