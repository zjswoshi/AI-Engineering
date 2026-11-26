# PostgreSQL

配置位于此目录的 `compose.yaml`。从项目根目录运行 `docker compose up -d postgres`，再用 `docker compose ps postgres` 查看健康状态。

- 使用 PostgreSQL 17 验证 SQL，评估事务、锁及查询计划。给真实业务创建独立的最小权限账号。
- `POSTGRES_USER` 是本地初始化管理员。初始化变量用于空数据卷，升级 PostgreSQL 主版本时先选定迁移方式和数据目录。

## Spring Boot 接入

在 `backend/` 运行 Spring Boot 时，先启动 PostgreSQL，再设置 `SPRING_PROFILES_ACTIVE=postgres`。本地可设置 `DEMO_ACCOUNTS_ENABLED=true` 初始化缺失的演示账号。连接自定义数据库时设置 `POSTGRES_JDBC_URL`、`POSTGRES_USER` 和 `POSTGRES_PASSWORD`。

- **数据库迁移。** Flyway 按版本执行 `database/postgres/migrations/` 中的 SQL。Compose 将迁移目录只读挂载给后端。`V1__create_accounts_and_roles.sql` 建账号和角色表并预置三种角色。表结构演进通过追加迁移记录。
- **数据访问。** `repository` 用 `JdbcClient` 执行参数化 SQL，`service` 管理业务事务。
- **演示账号。** 本地开发启动时，从配置读取密码并为缺失的演示账号保存密码哈希。后续启动沿用库里已有的账号和角色分配。
- **首轮验收。** 从空库启动后端，确认迁移完成并用演示账号登录。管理员将查看者改为操作员，重启 Spring Boot 后用该账号重新登录，核对数据库中的角色和提交权限仍然有效。

## PostgreSQL Skill

设计表结构、编写迁移、调整索引或分析慢查询时，先读本地 [Supabase Postgres Best Practices](skills/supabase-postgres-best-practices/SKILL.md)，再按任务查看 `references/` 中的规则。SQL 和扩展适用性以本项目的 PostgreSQL 版本为准。

随工程分发的版本是 `1.1.1`，固定到[上游提交 `c9be0e9`](https://github.com/supabase/agent-skills/tree/c9be0e931b7930f7d02126d04774d904c381e7d7/skills/supabase-postgres-best-practices)。完整的 MIT 许可证保存在 [LICENSE](skills/supabase-postgres-best-practices/LICENSE)。

检测项目的业务表设计见[示例项目草案](../../examples/ai-detection/design.md#postgresql-持久化)。
