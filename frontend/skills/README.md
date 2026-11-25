# 前端 Skill

先读 [`../AGENTS.md`](../AGENTS.md) 和[后台设计基线](../DESIGN.md#后台设计基线)。Vue 和 React 的 Agent 都可以读取这些参考资料。

| 本地入口 | 用途 | 上游及固定版本 |
| --- | --- | --- |
| [Modern Web Guidance](modern-web-guidance/SKILL.md) | HTML、CSS、DOM、可访问性和浏览器 API | [GoogleChrome/modern-web-guidance](https://github.com/GoogleChrome/modern-web-guidance/tree/2b4aa13a72dd47cd135b06ce5abfccd92e19b24f/skills/modern-web-guidance)，`2b4aa13` |
| [UI UX Pro Max](ui-ux-pro-max/SKILL.md) | 页面布局、交互和设计系统 | [nextlevelbuilder/ui-ux-pro-max-skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill/tree/8db36338810d0942567b97ce439affd1619fcf31/.claude/skills/ui-ux-pro-max)，`8db3633` |
| [React Best Practices](react-best-practices/SKILL.md) | React 模板的渲染、网络请求与包体积 | [Vercel agent-skills](https://github.com/vercel-labs/agent-skills/tree/5a37a6329fbad3666c3207392a6a7c0110e92efb/skills/react-best-practices)，`5a37a63`，MIT（上游 `SKILL.md` 声明） |
| [Vue Best Practices](vue-best-practices/SKILL.md) | Vue 3 单文件组件、响应式状态、组件数据流和 composables | [vuejs-ai/skills](https://github.com/vuejs-ai/skills/tree/198f0cd8f7f9c0a3d9b566ccc16de6677519ee78/skills/vue-best-practices)，`19.1.0` / `198f0cd`，[MIT](vue-best-practices/LICENSE) |
| [Playwright E2E](playwright-e2e/SKILL.md) | Vue 和 React 的登录、权限和检测操作浏览器回归 | 本项目维护，参考 [Playwright Best Practices](https://playwright.dev/docs/best-practices) |

Modern Web Guidance 的正文在 [guides/](modern-web-guidance/guides/)。可用 `rg --files frontend/skills/modern-web-guidance/guides` 搜索文件名，也可用 `rg -ni '关键词' frontend/skills/modern-web-guidance/guides` 查正文。UI UX Pro Max 的检索脚本使用 Python，资料也可以直接阅读。
