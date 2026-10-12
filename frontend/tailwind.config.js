/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f0f5fa',
          100: '#e1ecf5',
          200: '#c3d9eb',
          300: '#95bee0',
          400: '#609fd1',
          500: '#3a81c0',
          600: '#2967a5',
          700: '#1e4c7c',
          800: '#143557',
          900: '#0c223a',
          950: '#071526',
        }
      }
    },
  },
  plugins: [],
}
