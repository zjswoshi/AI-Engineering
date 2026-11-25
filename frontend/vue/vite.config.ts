import framework from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', 'API_PROXY_TARGET')

  return {
    plugins: [framework(), tailwindcss()],
    server: {
      proxy: {
        '/api/': env.API_PROXY_TARGET || 'http://127.0.0.1:8080',
      },
    },
  }
})
