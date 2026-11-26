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
middleware/redis/    可选缓存和本地持久卷
storage/minio/       已有 MinIO 社区版的本地兼容测试
docker/             通用前端镜像构建文件
nginx/               静态站点和 /api/ 同源反向代理
tools/               OpenCodeReview 源码和本地运行入口
compose.yaml         所选服务的容器编排
.env.example         本地环境变量示例
```

后端按 `controller → service → repository` 分层。各包职责和扩展约定见 [backend/AGENTS.md](backend/AGENTS.md)。

## 用 AI 创建新项目

把 [START-PROJECT.md](START-PROJECT.md) 直接发给能操作工作区的 AI。它会按决策依赖分轮厘清需求，等你确认双方理解一致后，再制定计划、实现并验证。已有项目描述时可以和文件一起发。

## 技术选型与开发入口

新项目按需选用下表中的目录，再从所选模块的 `AGENTS.md`、依赖清单和本地 Skill 开始开发。

| 模块 | 可选技术与使用方式 |
| --- | --- |
| 前端 | 从 [Vue + Element Plus](frontend/vue/AGENTS.md) 或 [React + Ant Design](frontend/react/AGENTS.md) 选一套。两套都使用 Tailwind CSS，各自维护依赖、API 客户端、样式和测试。 |
| 后端 | [Spring Boot](backend/AGENTS.md) 提供 Controller、Service、Repository 分层，以及权限、统一错误和日志的参考实现。 |
| 算法 | 在 [Python 算法目录](algorithm/AGENTS.md) 下按算法组织前处理、ONNX 模型加载和后处理。需要独立服务时，参考 FastAPI 的 HTTP 入口或 Kafka worker。 |
| 数据库 | 按业务数据模型选 [PostgreSQL](database/postgres/AGENTS.md)、[MySQL](database/mysql/AGENTS.md) 或 [MongoDB](database/mongodb/AGENTS.md)。PostgreSQL profile 已接入账号持久化。MySQL 提供 Flyway 迁移和可复制的 Spring Boot 配置，演示账号默认保存在内存。MongoDB 提供独立的服务配置。 |
| 工程配套 | 按需参考 [Docker](docker/AGENTS.md)、[Nginx](nginx/AGENTS.md) 和 [中间件](middleware/AGENTS.md) 配置。 |

## 新项目取用清单

以设备管理后台为例，选择 Vue、Spring Boot 和 PostgreSQL。以下裁剪在新项目的副本中进行。

1. **保留选中的模块和开发指引。** 保留 `frontend/vue/`、`backend/` 和 `database/postgres/`，以及对应的 `AGENTS.md` 和本地 Skill。Vue 也会用到 `frontend/AGENTS.md`、`frontend/DESIGN.md` 和 `frontend/skills/`。保留根 `README.md` 和 `AGENTS.md`，按所选模块更新其中的导航。根 `skills/` 和 `tools/` 按需取用。
2. **改成新项目的名称。** 修改 `frontend/vue/package.json` 的包名和 `frontend/vue/index.html` 的页面标题，同步更新 npm 锁文件。修改 `backend/pom.xml` 的 `groupId`、`artifactId` 和 `backend/src/main/resources/application.yaml` 的 `spring.application.name`。移动 `backend/src/main/java/com/example/starter/` 下的源码，更新 Java 的 `package`、`import` 和模块说明。若修改 POM 的 `finalName`，同时更新 `backend/Dockerfile` 中复制的 JAR 文件名。
3. **替换 AI 检测示例。** [裁剪步骤和后台验收](examples/ai-detection/README.md)。 从 `frontend/vue/src/router/sections.ts` 移除检测页面和工作台卡片的注册，再移除 Vue 项目中的 `src/views/DetectionsView.vue` 和 `src/features/detection/`，把 Playwright 用例和模拟 API 改成设备管理场景。后端移除检测相关的 Controller、Service、Repository、DTO、Model 和传输适配器，更新 `SecurityConfig.java` 中的 `/api/detections` 规则。按需保留账号与角色代码。在 `sections.ts` 注册 `/devices` 页面，后端按 Controller、Service、Repository 分层添加 `/api/devices`，并在 `database/postgres/migrations/` 追加设备表迁移，例如 `V2__create_devices.sql`。在 `contracts/` 记录新接口，并更新模块说明。
4. **先裁剪引用，再移除未选模块。** 按下表更新配置后，可以移除 `frontend/react/`、`algorithm/`、`database/mysql/`、`database/mongodb/`、`middleware/kafka/` 和 `middleware/redis/` 等未选目录。

| 配置 | 此例需要调整的内容 |
| --- | --- |
| [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | 前端矩阵 `matrix.app` 只留 `vue`。移除 `algorithm` 任务，保留 Spring Boot 和所选数据库的 Compose 校验。 |
| [`.github/workflows/publish-image.yml`](.github/workflows/publish-image.yml) | 如需发布镜像，组件选项和构建分支只留 `vue` 和 `backend`，保留 `docker/frontend.Dockerfile` 和 `nginx/default.conf`。 |
| [`compose.yaml`](compose.yaml) | `include` 只留 PostgreSQL。移除 `react`、`algorithm-http`、`algorithm-kafka` 服务。清理 `backend` 的 Kafka 依赖和算法、Kafka 环境变量。保留 PostgreSQL 的[叠加配置](database/postgres/backend.compose.yaml)，其中定义了数据库连接和迁移挂载。 |
| [`.env.example`](.env.example)、后端配置和 `pom.xml` | 删除未选服务的环境变量、`starter.detection` 和 `spring.kafka` 配置，以及移除检测适配器后用不到的 Kafka 和 HTTP 客户端依赖。保留 PostgreSQL 和所用账号的配置。 |

### 验证裁剪结果

在 `frontend/vue/` 执行 `npm.cmd ci`、`npm.cmd run build` 和更新后的 `npm.cmd run test:e2e`，在 `backend/` 执行 `mvn verify`。保留 Compose 时，在项目根目录运行以下命令，确认服务清单仅包含 `backend`、`vue`、`postgres`。

```powershell
docker compose -f compose.yaml -f database/postgres/backend.compose.yaml --profile '*' config --services
```

推送新项目后，确认 CI 只检查所选模块。若保留 PostgreSQL profile，可参考数据库模块的步骤，从空库验证 Flyway 迁移和账号权限。

### 改用 MySQL

保留 `database/mysql/`，在根 Compose 的 `include` 中保留 MySQL 配置，并保留 `.env.example` 的 `MYSQL_*` 变量。按 [MySQL 模块说明](database/mysql/AGENTS.md#接入-spring-boot) 接入 MySQL profile 和迁移，在 CI 中保留 MySQL 叠加配置校验并移除 PostgreSQL 对应的检查。裁剪完成后，下列命令应只列出 `backend`、`vue` 和 `mysql`。

```powershell
docker compose -f compose.yaml -f database/mysql/backend.compose.yaml --profile '*' config --services
```

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

Redis 配置可单独复制。已有 MinIO 社区版项目可选用独立的本地兼容测试配置。[Redis 模块](middleware/redis/AGENTS.md) · [MinIO 模块](storage/minio/AGENTS.md)

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
- 在根目录运行 `docker compose --profile '*' config --quiet`。服务启动后，访问 Vue 首页 `http://127.0.0.1:8088/` 和 `http://127.0.0.1:8088/api/health`，检查页面和 Nginx 转发。React 使用 `8089`。
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

[CI](.github/workflows/ci.yml) 在提交到 `main` 和向 `main` 发起 PR 时运行。Vue 和 React 分别做类型检查、构建和 Playwright 回归，浏览器测试使用模拟 API。后端用 JDK 21 执行 `mvn verify`，算法用 Python 3.12 检查锁文件、传输入口并用微型模型验证 CPU 推理，Compose 检查基础配置和两种关系数据库的叠加配置。复制模板后，按项目选型保留对应的构建任务和发布选项。

需要交付镜像时，在 GitHub 的 **Actions → Publish image → Run workflow** 选择 `main` 和一个组件。工作流用仓库的 `GITHUB_TOKEN` 将选定的 `linux/amd64` 镜像推到 GHCR。镜像标签为 `ghcr.io/<小写仓库所有者>/<小写仓库名>-<组件>:sha-<完整提交 SHA>`。Kafka 算法镜像使用 Compose 中的 worker 命令 `python -m starter_algorithm.worker`。算法 CPU 镜像选择 `algorithm-http-onnx-cpu` 或 `algorithm-kafka-onnx-cpu`。权重挂载和验证见[算法模块说明](algorithm/AGENTS.md#cpu-推理镜像)。

## AI 协作入口

各模块的 `AGENTS.md` 记录项目约定和本地 Skill 入口。接手某个模块时，先读该模块说明，再按任务打开对应的 `SKILL.md`。

| 目录 | 随项目提供 |
| --- | --- |
| [skills/](skills/README.md) | Superpowers 项目级开发流程 |
| [backend/skills/](backend/skills/README.md) | GitHub `java-springboot` 日常开发指引和 Piomin Spring Boot 插件 |
| [frontend/skills/](frontend/skills/README.md) | Modern Web Guidance、UI UX Pro Max、Vercel React Best Practices |
| [algorithm/skills/](algorithm/skills/README.md) | ONNX 推理部署、Hugging Face Community Evals |
| [database/postgres/skills/](database/postgres/AGENTS.md#postgresql-skill) | Supabase PostgreSQL Best Practices |
| [database/mysql/skills/](database/mysql/AGENTS.md#mysql-skill) | PlanetScale MySQL Skill |
