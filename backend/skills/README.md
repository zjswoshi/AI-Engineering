# 后端 Skill

从 [`../AGENTS.md`](../AGENTS.md) 进入。日常改动先读 `java-springboot`。涉及 Spring Security、测试或专项评审时，再读 Piomin 插件的对应章节。目录分层、依赖版本和接口仍以项目自己的说明和清单为准。

| 本地入口 | 上游及固定版本 |
| --- | --- |
| [java-springboot](java-springboot/SKILL.md) | [GitHub awesome-copilot](https://github.com/github/awesome-copilot/tree/93fc8b9ee3ef56b9b5e7261e6ce92d314cb2c1cf/skills/java-springboot)，`93fc8b9`，MIT |
| [Piomin Spring Boot 插件](claude-ai-spring-boot/skills/spring-boot/SKILL.md) | [piomin/claude-ai-spring-boot](https://github.com/piomin/claude-ai-spring-boot/tree/d127264449456bbf0c150c5b772705d27c446311)，`1.3.2` / `d127264`，Apache-2.0 |
| [后端权限回归](authorization-testing/SKILL.md) | 本项目维护，参考 [Spring Security MockMvc](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/index.html) |

Claude Code 可在已有客户端中直接读取上面的 Markdown。从项目根目录启动一次会话时，也可以加载随附的 Piomin 插件及其 Agent。

```powershell
claude --plugin-dir .\backend\skills\claude-ai-spring-boot
```

在 Codex 或其他 Agent 中可以读同一份 `SKILL.md` 和其本地引用。Piomin 的 `agents/` 提供 Claude Code 子代理定义。
