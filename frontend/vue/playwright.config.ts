import { defineConfig } from '@playwright/test'

const baseURL = process.env.E2E_BASE_URL || 'http://127.0.0.1:5273'

export default defineConfig({
  testDir: './tests',
  workers: 1,
  use: {
    baseURL,
    browserName: 'chromium',
    channel: process.env.E2E_CHROME_CHANNEL || undefined,
    trace: 'retain-on-failure',
  },
  webServer: process.env.E2E_BASE_URL ? undefined : {
    command: 'npm run dev -- --port 5273',
    url: baseURL,
    timeout: 60_000,
  },
})
