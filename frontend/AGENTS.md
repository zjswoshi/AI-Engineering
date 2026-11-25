# 前端

Vue 和 React 是独立模板，各自维护依赖、npm 锁文件、API 客户端和样式。前端源码的导入应留在各自项目目录内。演示凭证保留在当前页面内存。浏览器请求使用相对路径 `/api/…`，开发时由 Vite 转发，容器中由 Nginx 转发。前端环境变量仅用于公开配置。

- 设计或评审页面布局、信息层级和交互时，先读[后台设计基线](DESIGN.md#后台设计基线)，按需读取 `skills/ui-ux-pro-max/SKILL.md` 的后台和 Web 可访问性建议。
- 处理 HTML、CSS、DOM、可访问性或浏览器 API 时，先打开 `skills/modern-web-guidance/SKILL.md`，按具体能力从同目录的 `guides/` 查本地正文，再按项目的目标浏览器确认支持情况。采用语义化元素、键盘可达控件和可读的状态反馈。
- 加入组件库、路由或全局状态前，先判断当前功能是否需要。网络交互要能呈现请求中、成功和失败状态。
- 按[前端测试方案](DESIGN.md#前端测试方案)补齐改动涉及的浏览器用例。需要检查交互、截图或 API 请求时，可使用 Chrome DevTools MCP。
- 编写或维护浏览器回归时，读取 [Playwright E2E Skill](skills/playwright-e2e/SKILL.md)，在目标模板内维护用例。
- 在目标模板内运行 `npm ci` 和 `npm run build`。改动浏览器请求时，从 Vite 或 Nginx 的入口验证同源 API。

[本地 Skill 索引](skills/README.md)记录用途和上游版本。

上游参考

- [GoogleChrome Modern Web Guidance](https://github.com/GoogleChrome/modern-web-guidance/tree/main/skills/modern-web-guidance)
- [UI UX Pro Max Skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill)
- [Vite 开发代理](https://vite.dev/config/server-options.html#server-proxy)
