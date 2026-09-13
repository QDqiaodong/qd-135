/** @type {import('tailwindcss').Config} */

export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,ts,vue}"],
  theme: {
    container: {
      center: true,
    },
    extend: {
      colors: {
        'heritage': {
          'primary': '#1a365d',
          'secondary': '#d4a574',
          'accent': '#c27758',
          'dark': '#0d2137',
          'light': '#f5f0e8',
          'gold': '#b8860b',
          'terracotta': '#c27758',
        },
      },
      fontFamily: {
        'serif': ['Noto Serif SC', 'Playfair Display', 'serif'],
      },
    },
  },
  plugins: [],
};
