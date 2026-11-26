import { expect, test } from '@playwright/test'
import { installDemoApi, login } from './demo-api'

test('管理员可从工作台进入角色管理并退出', async ({ page }) => {
  await installDemoApi(page)
  await page.goto('/')
  await login(page, 'admin')
  await expect(page.getByRole('heading', { name: '工作台', exact: true })).toBeVisible()
  await page.getByRole('navigation', { name: '主导航' }).getByRole('link', { name: '角色管理' }).click()
  await expect(page.getByRole('combobox', { name: '角色' })).toBeEnabled()
  await page.getByRole('button', { name: '退出', exact: true }).click()
  await expect(page.getByRole('heading', { name: '登录' })).toBeVisible()
})
