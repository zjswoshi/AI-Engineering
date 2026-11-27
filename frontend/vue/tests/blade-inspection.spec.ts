import { expect, test } from '@playwright/test'
import { readFile } from 'node:fs/promises'

const sample = process.env.BLADE_E2E_ZIP
const adminPassword = process.env.BLADE_ADMIN_PASSWORD
const xhr = { 'X-Requested-With': 'XMLHttpRequest' }
const basic = (username: string, password: string) => ({
  ...xhr, Authorization: `Basic ${Buffer.from(`${username}:${password}`).toString('base64')}`,
})

test.use({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true })

test.skip(!sample || !adminPassword || !process.env.E2E_BASE_URL, 'Requires the running blade Compose stack and a real fixture ZIP')

test('uploads a real inspection, publishes the report, and isolates its artifacts', async ({ page, request }) => {
  test.setTimeout(180_000)
  const stamp = Date.now().toString(36)
  const owner = `owner-${stamp}`
  const other = `other-${stamp}`
  const password = 'Local-test-account-password'
  const admin = basic('admin', adminPassword!)
  for (const username of [owner, other]) {
    expect((await request.post('/api/admin/blade-operators', { headers: admin, data: { username, password } })).status()).toBe(201)
  }
  const ownerHeaders = basic(owner, password)
  const otherHeaders = basic(other, password)
  expect((await request.get('/api/detection-tasks')).status()).toBe(401)
  expect((await request.post('/api/admin/blade-operators', { headers: otherHeaders, data: { username: 'denied', password } })).status()).toBe(403)
  const parameters = new URLSearchParams({ farmName: '验证风场', turbineNumber: 'WT-001', filename: 'broken.zip' })
  const badZip = await request.post(`/api/detection-tasks?${parameters}`, {
    headers: { ...ownerHeaders, 'Content-Type': 'application/zip' }, data: Buffer.from('broken archive'),
  })
  expect(badZip.status()).toBe(400)
  expect(await badZip.json()).toMatchObject({ code: 'INVALID_ARCHIVE' })

  await page.goto('/')
  await page.getByLabel('账号', { exact: true }).fill(owner)
  await page.getByLabel('密码', { exact: true }).fill(password)
  await page.getByRole('button', { name: '登录' }).click()
  const shortcuts = page.getByRole('navigation', { name: '工作台快捷入口' })
  const inspection = shortcuts.getByRole('link', { name: '叶片检测' })
  await expect(inspection).toBeVisible()
  await expect(shortcuts.getByRole('link', { name: '巡检账号' })).toHaveCount(0)
  await inspection.press('Enter')
  await expect(page).toHaveURL(/\/detections$/)
  await expect(page.getByRole('heading', { name: '历史检测任务' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390)
  await page.getByRole('button', { name: '新建检测' }).click()
  await page.getByLabel('风场', { exact: true }).fill('验证风场')
  await page.getByLabel('风机编号', { exact: true }).fill('WT-001')
  await page.getByLabel('图片 ZIP', { exact: true }).setInputFiles(sample!)
  await page.getByRole('button', { name: '开始检测' }).click()
  await expect(page).toHaveURL(/\/detections\?task=[0-9a-f-]+/, { timeout: 30_000 })
  const id = new URL(page.url()).searchParams.get('task')!
  const taskPath = `/api/detection-tasks/${id}`
  await expect(page.getByRole('heading', { name: 'AI 检测结果' })).toBeVisible({ timeout: 100_000 })
  const summary = await (await request.get(`${taskPath}/report`, { headers: ownerHeaders })).json()
  expect(summary.imageCount).toBeGreaterThan(0)
  expect(Object.keys(summary.classes)).toHaveLength(10)
  expect(Object.values<number>(summary.classes).reduce((left, right) => left + right, 0)).toBe(summary.detectionCount)
  await expect(page.getByRole('definition').first()).toHaveText(String(summary.imageCount))
  const picker = page.getByRole('combobox', { name: '巡检图片' })
  await expect(picker).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390)
  const previous = await picker.inputValue()
  const options = await picker.locator('option').evaluateAll((elements: HTMLOptionElement[]) => elements.map((option) => option.value))
  const next = options.find((value) => value !== previous) ?? previous
  await picker.selectOption(next)
  await expect(picker).toHaveValue(next)
  const preview = page.getByRole('img', { name: /的检测标注/ })
  await expect(preview).toBeVisible()
  await expect.poll(() => preview.evaluate((image: HTMLImageElement) => image.naturalWidth)).toBeGreaterThan(0)
  const imageViewport = page.locator('#inspection-image-viewport')
  await page.getByRole('button', { name: '放大图片' }).tap()
  await expect.poll(() => imageViewport.evaluate((element) => element.scrollWidth - element.clientWidth)).toBeGreaterThan(0)
  await page.getByRole('button', { name: '适应屏幕' }).tap()
  await expect.poll(() => imageViewport.evaluate((element) => element.scrollWidth - element.clientWidth)).toBe(0)
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 15_000 }),
    page.getByRole('button', { name: '下载 PDF' }).tap(),
  ])
  expect(download.suggestedFilename()).toBe(`blade-report-${id}.pdf`)
  expect((await readFile((await download.path())!)).subarray(0, 5).toString()).toBe('%PDF-')
  expect((await (await request.get('/api/detection-tasks', { headers: otherHeaders })).json()).items).toEqual([])
  for (const endpoint of ['', '/report', '/images', '/images/0/preview', '/report.pdf']) {
    expect((await request.get(`${taskPath}${endpoint}`, { headers: otherHeaders })).status()).toBe(404)
    expect((await request.get(`${taskPath}${endpoint}`, { headers: admin })).status()).toBe(200)
  }
  await page.getByRole('link', { name: '返回检测任务' }).click()
  const latestReport = page.getByRole('button', { name: '查看报告' })
  await expect(latestReport).toBeInViewport()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390)
  await page.setViewportSize({ width: 1440, height: 900 })
  await latestReport.click()
  await expect(page.getByRole('list', { name: '逐图明细' })).toBeVisible()
})
