# 工程协作说明

这是供新项目按需取用的工程模板。`README.md` 提供技术选型、各模块的开发入口和 AI 检测示例参考。各模块的实际依赖版本以其清单和锁文件为准。

从零创建项目时，使用 [START-PROJECT.md](START-PROJECT.md) 发起需求访谈，并按其中的流程完成选型、实现和验证。

## 目录归属

| 目录 | 职责 |
| --- | --- |
| `frontend/vue/`、`frontend/react/` | 两套独立的 TypeScript 前端模板。新业务通常选择一套 |
| `backend/` | Spring Boot API、权限与检测任务 |
| `algorithm/` | Python 检测算法、FastAPI 和 Kafka worker |
| `examples/ai-detection/` | AI 检测示例的裁剪说明、业务与页面草案 |
| `contracts/` | HTTP API 和 Kafka 消息约定 |
| `middleware/` | Kafka 和 Redis 的可选运行配置 |
| `storage/minio/` | 已有 MinIO 社区版的本地兼容测试 |
| `database/` | MySQL、PostgreSQL 和 MongoDB 的独立配置及迁移归属 |
| `docker/`、`nginx/` | 前端镜像构建、静态文件服务和 API 反向代理 |
| `tools/` | 代码审查工具 |
| `skills/` | 跨模块的 AI 开发流程 |

## 工作规则

- 修改前阅读目标路径下适用的 `AGENTS.md`，跨目录变更逐个确认负责的模块。
- 将修改限定在承接需求的模块。两套前端只有在公共契约或模板本身变更时才需要同步。
- 以 `/api/` 作为浏览器访问后端的路径前缀。更改公共接口时检查前端调用与代理，并记录请求和响应的变化。
- 用 `.env.example` 登记配置名和开发默认值。真实凭证、业务数据、模型权重和构建产物留在版本控制之外。
- 需要设计、计划、调试或审查等通用工程流程时，从 [Superpowers 本地入口](skills/README.md) 选择对应的 Skill。
- 从模块 `AGENTS.md` 进入该模块 `skills/` 下的本地 Skill，按任务读取 `SKILL.md` 及相关引用。工程分层、接口契约和依赖以本项目的文档与清单为准。
- 按改动范围验证所选模块。涉及检测示例的跨模块改动时，参考 `README.md` 里的冒烟检查。交付时注明实际执行的命令和仍待运行的检查。
