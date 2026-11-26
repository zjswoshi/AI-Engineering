# Spring Boot 后端

后端是单个 Maven 工程，使用 `controller → service → repository` 分层。`StarterApplication` 位于根包 `com.example.starter`，运行配置位于 `src/main/resources/application.yaml`。

| Java 包 | 职责 |
| --- | --- |
| `controller` | HTTP 路由、请求校验、调用者身份提取和响应映射 |
| `dto` | HTTP 请求和响应的数据结构 |
| `service` | 业务流程、权限范围与事务边界 |
| `repository` | 数据读写和查询 |
| `model` | 服务内部使用的业务模型 |
| `config` | Spring Security 配置和演示账号 |
| `filter` | API 请求编号、响应状态和耗时日志 |
| `exception` | API 错误码、ProblemDetail 构造和 MVC 统一异常处理 |
| `integration/` | 与外部服务或消息系统交互的适配器 |

检测模块的接入和裁剪步骤见[示例说明](../examples/ai-detection/README.md)。

- 新增接口时先明确调用者、输入和状态码。权限和任务可见范围以服务端校验为准，前端隐藏只是交互设计。
- `AccountRepository` 提供账号认证读取和角色分配，启用 `postgres` profile 后使用 `PostgresAccountRepository`，默认使用内存实现。 [PostgreSQL 启动与迁移](../database/postgres/AGENTS.md#spring-boot-接入)
- 日常开发先读 `skills/java-springboot/SKILL.md`，按需阅读 REST、配置、数据访问或 Kafka 章节。涉及 Spring Boot 4 测试、安全或后端专项评审时，可查 `skills/claude-ai-spring-boot/skills/`。目录结构遵循本文的分层约定。
- 验证接口权限、任务归属或结果访问时，读取[后端权限回归 Skill](skills/authorization-testing/SKILL.md)的测试矩阵。
- 运行 `mvn verify`。修改权限时验证未认证、权限不足、授权成功和用户间数据隔离。修改传输时通过启动方式提交样本并查询结果。

[本地 Skill 索引](skills/README.md)记录版本和 Claude Code 的本地加载命令。

## API 错误处理

使用 `ApiError` 登记稳定错误码和公开提示。Controller 可抛出 `ApiException`，由 `ApiExceptionHandler` 统一转换为 `ProblemDetail`。Spring Security 用同一个错误定义生成认证和授权失败的响应。响应体中的 `requestId` 对应响应头与日志。字段约定见[错误响应契约](../contracts/detection.md#错误响应)。

## 日志

Spring Boot 将日志以 JSON 输出到标准输出。Compose 启动时在项目根目录用 `docker compose logs -f backend` 查看。

- API 响应头 `X-Request-ID` 由后端生成，对应日志中的 `requestId`。请求日志记录方法、已匹配的路由模板、状态码和耗时。健康检查成功记录在 `DEBUG`。
- 异步任务日志记录可关联的任务 ID，便于从提交追踪到完成。
- 角色更新日志记录操作方、目标账号、变更前后的角色。日志字段只放排查所需的标识和状态，凭证、请求正文、检测样本及算法结果留在日志之外。

写日志时参考[本地日志 Skill](skills/claude-ai-spring-boot/skills/logging-patterns/SKILL.md)。

上游参考

- [Anthropic MCP Builder](https://github.com/anthropics/skills/tree/main/skills/mcp-builder)
- [Spring Boot Web](https://docs.spring.io/spring-boot/reference/web/servlet.html)
- [Spring Security HTTP Basic](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/basic.html)
