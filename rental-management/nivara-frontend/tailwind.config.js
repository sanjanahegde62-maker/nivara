/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // Nivara brand palette
        ivory: {
          50:  '#fdfcf9',
          100: '#faf7f0',
          200: '#f5f0e6',
          300: '#ede4d4',
        },
        terracotta: {
          50:  '#fdf3ef',
          100: '#fbe4da',
          200: '#f7c9b5',
          300: '#f0a484',
          400: '#e77d52',
          500: '#c8652e',   // primary accent
          600: '#a84f21',
          700: '#8a3c17',
        },
        sage: {
          50:  '#f3f5f0',
          100: '#e5ead9',
          200: '#c8d4b3',
          300: '#9db07e',
          400: '#738d50',   // restrained sage
          500: '#556b38',
        },
        warm: {
          50:  '#fdf8f4',
          100: '#f4ede3',
          200: '#e8d8c4',
          300: '#d6bea0',
          900: '#2c1f14',  // warm dark text
          800: '#3d2b1a',
          700: '#5c4130',
        },
        surface: '#f7f5f0',
        border: '#e5dfd5',
      },
      fontFamily: {
        sans: ['"Inter"', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'sans-serif'],
        display: ['"Playfair Display"', 'Georgia', 'serif'],
      },
      boxShadow: {
        card: '0 1px 3px 0 rgba(44,31,20,0.07), 0 1px 2px -1px rgba(44,31,20,0.05)',
        'card-hover': '0 4px 12px 0 rgba(44,31,20,0.10), 0 2px 4px -1px rgba(44,31,20,0.06)',
        soft: '0 2px 8px 0 rgba(44,31,20,0.08)',
      },
      borderRadius: {
        xl: '1rem',
        '2xl': '1.25rem',
      },
    },
  },
  plugins: [require('@tailwindcss/forms')],
};
