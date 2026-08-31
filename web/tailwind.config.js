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
        'cream-text': '#f5f5dc',
        'win-green': '#4caf50',
        'lose-red': '#f44336'
      }
    },
  },
  plugins: [],
}
