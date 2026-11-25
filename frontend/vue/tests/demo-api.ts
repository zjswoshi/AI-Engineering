import { expect, type Page } from '@playwright/test'

type Role = 'VIEWER' | 'OPERATOR' | 'ADMIN'

export const useRealApi = process.env.E2E_REAL_API === '1'

export async function installDemoApi(page: Page) {
  if (useRealApi) return

  let analystRole: Role = 'VIEWER'
  let nextId = 1
  const jobs: { id: string; sample: string; owner: string; createdAt: string; status: 'COMPLETED'; result: { label: string; score: number } }[] = []
  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    const method = request.method()
    const reply = (status: number, json: unknown) => route.fulfill({ status, json })
    const credentials = request.headers().authorization
    const username = credentials === `Basic ${Buffer.from('admin:local-admin').toString('base64')}` ? 'admin'
      : credentials === `Basic ${Buffer.from('analyst:local-analyst').toString('base64')}` ? 'analyst' : null
    if (!username) return reply(401, {})
    const role = username === 'admin' ? 'ADMIN' : analystRole

    if (path === '/api/me' && method === 'GET') return reply(200, { username, role })
    if (path === '/api/admin/analyst') {
      if (role !== 'ADMIN') return reply(403, {})
      if (method === 'PUT') analystRole = request.postDataJSON().role
      if (method === 'GET' || method === 'PUT') return reply(200, { username: 'analyst', role: analystRole })
    }
    if (path === '/api/detections') {
      if (method === 'GET') return reply(200, jobs.filter((job) => role === 'ADMIN' || job.owner === username))
      if (method === 'POST') {
        if (role === 'VIEWER') return reply(403, {})
        const { sample } = request.postDataJSON()
        const job = { id: String(nextId++), sample, owner: username, createdAt: new Date().toISOString(), status: 'COMPLETED' as const, result: { label: 'demo', score: 0.823 } }
        jobs.unshift(job)
        return reply(201, job)
      }
    }
    return reply(404, {})
  })
}

export async function login(page: Page, username: 'admin' | 'analyst') {
  const password = username === 'admin' ? process.env.E2E_ADMIN_PASSWORD || 'local-admin' : process.env.E2E_ANALYST_PASSWORD || 'local-analyst'
  await page.getByLabel('账号', { exact: true }).fill(username)
  await page.getByLabel('密码', { exact: true }).fill(password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
}

export async function setAnalystRole(page: Page, role: 'VIEWER' | 'OPERATOR') {
  const password = process.env.E2E_ADMIN_PASSWORD || 'local-admin'
  const result = await page.request.put('/api/admin/analyst', {
    headers: { Authorization: `Basic ${Buffer.from(`admin:${password}`).toString('base64')}` },
    data: { role },
  })
  expect(result.ok()).toBeTruthy()
}
