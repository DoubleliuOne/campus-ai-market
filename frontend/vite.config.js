import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return undefined
          }
          if (
            id.includes('/node_modules/vue/') ||
            id.includes('/node_modules/vue-router/')
          ) {
            return 'vendor-vue'
          }
          if (id.includes('/node_modules/element-plus/')) {
            return 'vendor-element'
          }
          if (id.includes('/node_modules/lucide-vue-next/')) {
            return 'vendor-icons'
          }
          if (
            id.includes('/node_modules/marked/') ||
            id.includes('/node_modules/dompurify/')
          ) {
            return 'vendor-markdown'
          }
          if (id.includes('/node_modules/axios/')) {
            return 'vendor-http'
          }
          return 'vendor-misc'
        },
      },
    },
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
