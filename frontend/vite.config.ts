import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the API is proxied, so the browser talks to one origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
