import { expect, test } from '@playwright/test'
import { installDemoApi, login, setAnalystRole, useRealApi } from './demo-api'

test.beforeEach(async ({ page }) => {
  await installDemoApi(page)
  if (useRealApi) await setAnalystRole(page, 'VIEWER')
})

test.afterEach(async ({ page }) => {
  if (useRealApi) await setAnalystRole(page, 'VIEWER')
})

test('登录后回到原页面，退出后清除页面会话', async ({ page }) => {
  await page.goto('/detections?new=1')
  await expect(page).toHaveURL(/\/login\?returnTo=/)
  await login(page, 'analyst')
  await expect(page).toHaveURL(/\/detections\?new=1$/)
  await expect(page.getByRole('heading', { name: '检测任务', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '开始检测' })).toHaveCount(0)
  await page.getByRole('button', { name: '退出', exact: true }).click()
  await expect(page.getByRole('heading', { name: '登录' })).toBeVisible()
})

test('查看者看不到角色管理，直接进入时显示 403', async ({ page }) => {
  await page.goto('/login')
  await login(page, 'analyst')
  await expect(page.getByRole('heading', { name: '工作台', exact: true })).toBeVisible()
  const navigation = page.getByRole('navigation', { name: '主导航' })
  await expect(navigation).toBeVisible()
  await expect(navigation.getByRole('link', { name: '角色管理' })).toHaveCount(0)
  await page.evaluate(() => {
    window.history.pushState({}, '', '/admin/roles')
    window.dispatchEvent(new PopStateEvent('popstate'))
  })
  await expect(page).toHaveURL(/\/403$/)
  await expect(page.getByRole('heading', { name: '403 无权限' })).toBeVisible()
  if (useRealApi) {
    const password = process.env.E2E_ANALYST_PASSWORD || 'local-analyst'
    const headers = { Authorization: `Basic ${Buffer.from(`analyst:${password}`).toString('base64')}` }
    expect((await page.request.get('/api/admin/analyst', { headers })).status()).toBe(403)
    expect((await page.request.post('/api/detections', { headers, data: { sample: 'viewer-test' } })).status()).toBe(403)
  }
})

test('管理员分配角色后，操作员提交样本并再次打开结果', async ({ page }) => {
  await page.goto('/admin/roles')
  await login(page, 'admin')
  await expect(page.getByRole('heading', { name: '角色管理' })).toBeVisible()
  const role = page.getByRole('combobox', { name: '角色' })
  await expect(role).toBeEnabled()
  await role.press('ArrowDown')
  await expect(role).toHaveAttribute('aria-expanded', 'true')
  await page.getByText('操作员', { exact: true }).last().click()
  await page.getByRole('button', { name: '保存权限' }).click()
  await expect(page.getByRole('status').filter({ hasText: '权限已更新' })).toBeVisible()
  await page.getByRole('button', { name: '退出', exact: true }).click()
  await login(page, 'analyst')

  await page.getByText('新建检测任务', { exact: true }).click()
  const sample = `sample-e2e-${Date.now()}`
  await page.getByLabel('样本编号').fill(sample)
  await page.getByRole('button', { name: '开始检测' }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText(sample, { exact: true })).toBeVisible()
  await expect(dialog.getByText(/检出项/)).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(dialog).toBeHidden()
  await page.getByRole('row').filter({ hasText: sample }).getByRole('button', { name: '查看结果' }).click()
  await expect(dialog.getByText(/置信度/)).toBeVisible()
})
