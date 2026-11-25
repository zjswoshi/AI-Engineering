# 项目级 Skill

[Superpowers](superpowers/README.md) 提供需求讨论、实施计划、系统化调试、测试驱动开发和代码审查流程。仓库内的完整快照来自 [`obra/superpowers` v7.0.2](https://github.com/obra/superpowers/tree/22f0b463e7a5e579904dd6211a520321035526f7)，提交为 `22f0b463e7a5e579904dd6211a520321035526f7`。许可见 [LICENSE](superpowers/LICENSE)。

Codex 和其他能读取项目文件的 Agent 可从 [using-superpowers](superpowers/skills/using-superpowers/SKILL.md) 进入，再读取对应任务的本地 `SKILL.md`。进入业务模块时，同时遵循该模块的 `AGENTS.md`。

在项目根目录可用已有的 Claude Code 客户端加载随附插件。

```powershell
claude --plugin-dir .\skills\superpowers
```
