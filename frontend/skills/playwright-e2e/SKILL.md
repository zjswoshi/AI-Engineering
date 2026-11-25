---
name: playwright-e2e
description: 编写和维护 Vue 或 React 模板的 Playwright 浏览器回归。适用于登录回跳、角色页面、检测提交、文件上传、历史筛选和结果预览的改动。
---

# Playwright 浏览器回归

先读[前端测试方案](../../DESIGN.md#前端测试方案)和[检测接口契约](../../../contracts/detection.md)。在本次改动的模板目录使用已有 `playwright.config.ts`，从 `tests/workbench.spec.ts` 和 `tests/demo-api.ts` 复用场景与登录、模拟 API 辅助函数。Vue 和 React 各自维护用例、配置与夹具。

## 选择验证方式

| 要验证的行为 | 运行方式 |
| --- | --- |
| 页面导航、表单反馈、筛选状态和预览交互 | 使用默认模拟 API，模拟响应遵循接口契约。 |
| Vite 代理、Spring Security 和真实 API 响应 | 按[工程启动说明](../../../README.md)启动隔离的演示实例，再设置 `E2E_REAL_API=1`。现有用例会修改 `analyst` 角色并恢复为 `VIEWER`，对同一实例依次运行两套模板。 |

## 写用例

- 从用户可见的结果写断言。登录回跳检查目标 URL 和页面内容，权限页面检查菜单以及在已登录会话中访问受限路由的结果。演示凭证存在页面内存，完整刷新会清除会话。
- 优先使用 `getByRole` 和 `getByLabel`，用定位器断言等待页面状态。例如提交检测后等待任务行或结果弹窗出现，用 `expect(...).toBeVisible()` 代替固定延时。
- 当前 `/api/detections` 用例提交样本编号。文件接口实现后，用 `setInputFiles` 和小型测试夹具覆盖 ZIP 与视频的入口、提交反馈及失败重试。需要验证历史页时，检查日期、任务类型、检出项和任务名称筛选，以及关闭预览后保留的筛选与分页。
- 真实接口改动记录浏览器测试覆盖的 API 范围。角色判定和任务归属以服务端回归为准。

## 执行与定位失败

在 `frontend/vue/` 或 `frontend/react/` 运行。`--list` 用于核对测试收集，实际运行需要 Chromium 或配置了 `E2E_CHROME_CHANNEL` 的本地 Chrome。浏览器准备方式见工程启动说明。

```powershell
npm.cmd run test:e2e -- --list
npm.cmd run test:e2e
```

失败后打开 `test-results/` 中对应的 `trace.zip`，核对定位器、页面状态和请求响应，再重跑受影响的用例。

参考 [Playwright Best Practices](https://playwright.dev/docs/best-practices)。
