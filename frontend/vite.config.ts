import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5180,
    strictPort: true,
    // The browser calls /api on the Vite dev server, which forwards to Spring Boot.
    // Same origin for the browser = no CORS problems in development.
    proxy: {
      '/api': 'http://localhost:8081',
      '/actuator': 'http://localhost:8081',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
})
