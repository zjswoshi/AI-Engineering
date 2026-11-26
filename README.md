# 工程模板（含 AI 检测示例）

面向新项目的工程骨架，提供前端、后端、算法、数据库和部署配置。每个模块有自己的开发说明，相关 Skill 随工程分发。仓库附带 AI 检测示例，用模拟结果展示登录、角色分配、提交检测和查看结果的流程。

```text
frontend/
  vue/               Vue 3 + TypeScript + Vite
  react/             React 19 + TypeScript + Vite
backend/             Spring Boot 4 + Spring Security + Java 21
  src/main/java/com/example/starter/
    controller/      HTTP 接口
    service/         业务流程
    repository/      数据访问
    model/           任务与结果模型
    config/          Spring 配置
    integration/     HTTP 和 Kafka 算法接入
algorithm/           Python 3.12+、FastAPI 入口、Kafka worker
contracts/           HTTP API 和 Kafka 消息约定
database/           MySQL、PostgreSQL、MongoDB 的独立运行配置
middleware/kafka/    单节点开发用 Kafka
docker/             通用前端镜像构建文件
nginx/               静态站点和 /api/ 同源反向代理
tools/               冒烟检查和代码审查
compose.yaml         容器编排与 Redis 配置
.env.example         本地环境变量示例
```

后端按 `controller → service → repository` 分层。各包职责和扩展约定见 [backend/AGENTS.md](backend/AGENTS.md)。

## 技术选型与开发入口

新项目按需选用下表中的目录，再从所选模块的 `AGENTS.md`、依赖清单和本地 Skill 开始开发。

| 模块 | 可选技术与使用方式 |
| --- | --- |
| 前端 | 从 [Vue + Element Plus](frontend/vue/AGENTS.md) 或 [React + Ant Design](frontend/react/AGENTS.md) 选一套。两套都使用 Tailwind CSS，各自维护依赖、API 客户端、样式和测试。 |
| 后端 | [Spring Boot](backend/AGENTS.md) 提供 Controller、Service、Repository 分层，以及权限、统一错误和日志的参考实现。 |
| 算法 | 在 [Python 算法目录](algorithm/AGENTS.md) 下按算法组织前处理、ONNX 模型加载和后处理。需要独立服务时，参考 FastAPI 的 HTTP 入口或 Kafka worker。 |
| 数据库 | 按业务数据模型选 [PostgreSQL](database/postgres/AGENTS.md)、[MySQL](database/mysql/AGENTS.md) 或 [MongoDB](database/mongodb/AGENTS.md)。PostgreSQL 有 Spring Boot 和 Flyway 接入示例，其他引擎提供各自的服务配置。 |
| 工程配套 | 按需参考 [Docker](docker/AGENTS.md)、[Nginx](nginx/AGENTS.md) 和 [中间件](middleware/AGENTS.md) 配置。 |

例如做设备管理后台，可以选 Vue、Spring Boot 和 PostgreSQL，从 Vue 的 `src/router/sections.ts` 注册设备页，在后端按 Controller、Service、Repository 分层添加接口，再把设备表变更放进所选数据库的迁移目录。AI 检测示例展示前端页面、后端接口和算法入口的衔接方式，后续项目按自己的业务编写接口与数据模型。

## AI 检测示例运行参考

按下文的本机开发说明分别运行选用的模块。以下地址对应仓库中的 Compose 示例配置。

| 入口 | 默认地址 |
| --- | --- |
| Vue | `http://127.0.0.1:8088` |
| React | `http://127.0.0.1:8089` |
| Spring Boot 健康检查 | `http://127.0.0.1:8080/api/health` |

| 开发账号 | 默认密码 | 初始权限 |
| --- | --- | --- |
| `admin` | `local-admin` | 查看全部任务、提交检测、修改 `analyst` 的角色 |
| `analyst` | `local-analyst` | 查看自己的任务，授予操作员角色后可提交 |

用 `admin` 登录后，可在“账号权限”将 `analyst` 切换为操作员。提交任意样本编号，例如 `sample-001`。HTTP 模式当次返回模拟结果，Kafka 模式可点击“刷新”读取消费后的结果。详情见 [接口契约](contracts/detection.md)。

将 `.env.example` 复制为 `.env` 后可调整端口和凭证。后端默认使用内存账号。启用 PostgreSQL profile 后，Flyway 建立账号和角色表，Spring Boot 重启后从 PostgreSQL 读取账号和角色分配。样本检测记录保存在后端进程内存。HTTP Basic 和默认账号仅用于本地演示，部署前应接入实际身份系统和 HTTPS。

本地演示账号由 `DEMO_ACCOUNTS_ENABLED` 控制，首次创建时读取 `DEMO_ADMIN_PASSWORD` 和 `DEMO_ANALYST_PASSWORD`。已有账号沿用数据库里的密码哈希和角色分配。单独启动 PostgreSQL 及本机运行后端的方式见 [PostgreSQL 模块说明](database/postgres/AGENTS.md#spring-boot-接入)。

## 按需启动数据服务

```powershell
docker compose up -d mysql
docker compose up -d postgres
docker compose up -d mongodb
docker compose up -d redis
```

端口分别为 `3306`、`5432`、`27017`、`6379`，默认绑定到回环地址。接入业务时，根据数据模型在关系库与文档库之间选型，再把持久化接到后端。Kafka 在 Docker 内部网络使用 `kafka:9092`，消息和消费位点保存在 `kafka_data` 命名卷。启用 Kafka 时，Compose 等 broker 就绪再启动后端，后端健康检查通过后启动算法 worker。

## 本机开发参考

按选用的模块查阅以下命令。前端使用 Node.js 24，Spring Boot 使用 JDK 21 和 Maven，算法使用 Python 3.12+ 和 uv。

```powershell
# algorithm/，启动 HTTP 算法进程
uv run --locked --extra http uvicorn starter_algorithm.http:app --port 8000

# backend/，默认访问本机的 HTTP 算法进程
mvn spring-boot:run

# frontend/vue/ 或 frontend/react/
npm.cmd ci
npm.cmd run dev
```

Vue 开发端口为 `5173`，React 为 `5174`。Vite 默认转发 `/api/` 到 `http://127.0.0.1:8080`。可在所选前端目录的 `.env.local` 设置 `API_PROXY_TARGET` 并重启 Vite。容器配置使用根目录的 `.env`。

两套前端各自维护 `src/` 和 `tests/`。通用后台代码位于 `layouts/`、`features/auth/` 和 `features/roles/`。Vue 使用 `router/` 与 `views/`，React 使用 `app/` 与 `pages/`。检测示例在各端的 `features/detection/`，注册入口分别是 `src/router/sections.ts` 和 `src/app/sections.ts`。移除示例时，先删掉注册项，再删掉检测模块及对应的路由页面。

## 验证与扩展

- 前端在各自目录运行 `npm.cmd ci`、`npm.cmd run build`。后端在 `backend/` 运行 `mvn verify`。
- 在 `algorithm/` 运行 `uv run --locked python -m starter_algorithm` 验证 Python 包入口。
- 在根目录运行 `docker compose --profile '*' config --quiet`。服务启动后，用 `./tools/smoke.ps1 -BaseUrl http://127.0.0.1:8088 -Frontend` 检查 Vue 首页和经 Nginx 转发的健康接口。React 使用 `8089`。
- 如果采用其他后端技术，按新项目的业务定义接口和部署配置。需要复用检测示例页面时，参考现有 [检测接口契约](contracts/detection.md) 检查兼容性。

### 前端浏览器回归

在选定的前端目录执行一次 `npx.cmd playwright install chromium`，再运行 `npm.cmd run test:e2e`。Playwright 自动启动该模板的 Vite 服务，默认使用测试内的模拟 API。设置 `E2E_CHROME_CHANNEL=chrome` 可以使用本机的 Chrome。

本机启动独立的 HTTP 算法和 Spring Boot 演示实例后，可在相同终端设置 `E2E_REAL_API=1` 再运行 `npm.cmd run test:e2e`，通过 Vite 代理验证真实 API。该测试会修改演示账号 `analyst` 的角色，结束时设回 `VIEWER`。真实接口对同一演示实例执行时，请依次运行两套模板的用例。自定义后端地址和演示密码时，可设置 `API_PROXY_TARGET`、`E2E_ADMIN_PASSWORD` 和 `E2E_ANALYST_PASSWORD`。

### 可选代码审查

OpenCodeReview 源码随项目保存在 `tools/opencodereview/source/`。Windows x64 可直接从项目根目录调用以下命令。首次运行解包随附的可执行文件并校验 SHA-256。

```powershell
.\tools\ocr.ps1 --version
.\tools\ocr.ps1 rules check frontend/vue/src/App.vue
.\tools\ocr.ps1 review
```

审查使用根目录的 [.opencodereview/rule.json](.opencodereview/rule.json)。`rules check` 需要 Git 仓库，`review` 还需要基线提交。原生审查首次需设置模型和凭证，所选模型服务会收到改动及所需的源码上下文。其他平台的源码构建命令见 [OpenCodeReview 工具说明](tools/opencodereview/README.md)。

## CI/CD

[CI](.github/workflows/ci.yml) 在提交到 `main` 和向 `main` 发起 PR 时运行。Vue 和 React 分别做类型检查、构建和 Playwright 回归，浏览器测试使用模拟 API。后端用 JDK 21 执行 `mvn verify`，算法用 Python 3.12 检查锁文件、传输入口和 ONNX Runtime CPU 环境，Compose 检查基础配置和 PostgreSQL 叠加配置。复制模板后，按项目选型保留对应的构建任务和发布选项。

需要交付镜像时，在 GitHub 的 **Actions → Publish image → Run workflow** 选择 `main` 和一个组件。工作流用仓库的 `GITHUB_TOKEN` 将选定的 `linux/amd64` 镜像推到 GHCR。镜像标签为 `ghcr.io/<小写仓库所有者>/<小写仓库名>-<组件>:sha-<完整提交 SHA>`。Kafka 算法镜像使用 Compose 中的 worker 命令 `python -m starter_algorithm.worker`。在目标环境接入镜像、模型权重和配置。

## AI 协作入口

各模块的 `AGENTS.md` 记录项目约定和本地 Skill 入口。接手某个模块时，先读该模块说明，再按任务打开对应的 `SKILL.md`。

| 目录 | 随项目提供 |
| --- | --- |
| [skills/](skills/README.md) | Superpowers 项目级开发流程 |
| [backend/skills/](backend/skills/README.md) | GitHub `java-springboot` 日常开发指引和 Piomin Spring Boot 插件 |
| [frontend/skills/](frontend/skills/README.md) | Modern Web Guidance、UI UX Pro Max、Vercel React Best Practices |
| [algorithm/skills/](algorithm/skills/README.md) | ONNX 推理部署、Hugging Face Community Evals |
| [database/postgres/skills/](database/postgres/AGENTS.md#postgresql-skill) | Supabase PostgreSQL Best Practices |
