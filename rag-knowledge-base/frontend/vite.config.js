import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/chat': {
        target: 'http://localhost:8083',
        changeOrigin: true
      },
      '/api/doc': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
