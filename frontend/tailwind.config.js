/**
 * Prastaav design tokens. Documented in docs/design/README.md.
 * Text and button colours use the 600/700 steps so they meet WCAG AA on white.
 * @type {import('tailwindcss').Config}
 */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#EAF7F5',
          100: '#CFEFEA',
          200: '#A3E0D8',
          300: '#6FCBBF',
          400: '#3BB3A5',
          500: '#0F9D8F',
          600: '#0C8478',
          700: '#0A6B62',
          800: '#0A564F',
          900: '#0A4741',
          DEFAULT: '#0F9D8F',
        },
        accent: {
          50: '#EFF4FF',
          100: '#DBE7FE',
          500: '#3B82F6',
          600: '#2563EB',
          700: '#1D4ED8',
          DEFAULT: '#2563EB',
        },
        ink: '#0B1F4D',
        muted: '#64748B',
        line: '#E2E8F0',
        canvas: '#F5F9FC',
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans Variable"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        field: '12px',
        card: '16px',
      },
      boxShadow: {
        card: '0 1px 2px rgba(11, 31, 77, 0.04), 0 12px 32px -16px rgba(11, 31, 77, 0.14)',
        pop: '0 4px 8px rgba(11, 31, 77, 0.06), 0 24px 48px -12px rgba(11, 31, 77, 0.22)',
        focus: '0 0 0 4px rgba(15, 157, 143, 0.18)',
      },
      backgroundImage: {
        'brand-gradient': 'linear-gradient(90deg, #0C8478 0%, #2563EB 100%)',
        'brand-text': 'linear-gradient(90deg, #0F9D8F 0%, #2563EB 100%)',
        'canvas-glow':
          'radial-gradient(60rem 30rem at 0% 0%, rgba(15, 157, 143, 0.08), transparent 60%), radial-gradient(50rem 30rem at 100% 0%, rgba(37, 99, 235, 0.07), transparent 60%)',
        'cta-band': 'linear-gradient(100deg, #EAF7F5 0%, #EFF6FF 100%)',
      },
      maxWidth: {
        canvas: '1440px',
      },
    },
  },
  plugins: [],
};
