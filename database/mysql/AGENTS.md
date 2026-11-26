# MySQL

配置位于此目录的 `compose.yaml`。从项目根目录运行 `docker compose up -d mysql`，再用 `docker compose ps mysql` 查看健康状态。

- 使用 MySQL 8.4 验证 SQL，明确字符集、排序规则和索引长度等取值。运行时使用 `MYSQL_USER` 对应的应用账号。
- 初始化账号和库依赖首次启动时的空数据卷。已有数据库的账号变更使用显式 SQL 和对应的应用配置更新。

## 迁移和账号表

用 [migrations/](migrations/) 保存 Flyway 版本化脚本。`V1__create_accounts_and_roles.sql` 创建 `app_user`、`app_role` 和 `app_user_role`，预置 `ADMIN`、`OPERATOR` 和 `VIEWER`。`app_user_role` 以用户 ID 为主键，每个用户最多分配一个角色。此迁移只初始化角色，应用负责创建账号并写入密码哈希。后续变更追加新版本 SQL。

## 接入 Spring Boot

1. 将 [application-mysql.yaml](spring-boot/application-mysql.yaml) 复制到 `backend/src/main/resources/`。在 `backend/pom.xml` 保留 `spring-boot-starter-flyway`，添加 `org.flywaydb:flyway-mysql` 和 `com.mysql:mysql-connector-j`。当前 Spring Boot 父 POM 管理这两个依赖的版本。只采用 MySQL 时可移除 PostgreSQL 驱动和 Flyway PostgreSQL 模块。
2. 在 `backend/src/main/resources/application.yaml` 修改排除数据源自动配置的 profile 条件。仅使用 MySQL 时把 `"!postgres"` 改为 `"!mysql"`，同时保留两种可选配置时改为 `"!postgres & !mysql"`。每次运行只启用一种数据库 profile。
3. 本机运行时先启动 `mysql` 服务，再从 `backend/` 使用 `SPRING_PROFILES_ACTIVE=mysql` 启动 Spring Boot。用 `MYSQL_JDBC_URL`、`MYSQL_USER` 和 `MYSQL_PASSWORD` 覆盖连接配置。用 Compose 时，叠加 [backend.compose.yaml](backend.compose.yaml)，它负责等待 MySQL 健康并只读挂载迁移目录。

```powershell
docker compose -f compose.yaml -f database/mysql/backend.compose.yaml --profile mysql config --quiet
docker compose -f compose.yaml -f database/mysql/backend.compose.yaml --profile mysql up -d mysql backend
```

在空库验证 Flyway 的 `flyway_schema_history` 及三个初始角色。当前后端在 `mysql` profile 下仍使用内存账号。需要持久化账号时，为 MySQL 实现 `AccountRepository`，同时调整内存实现的 profile 条件。同名的 PostgreSQL 表采用 UUID 用户 ID，MySQL 迁移使用自增 `BIGINT`，数据访问代码按所用引擎处理。

## MySQL Skill

设计表、写迁移、评估索引、排查慢查询或事务锁时，先读 [PlanetScale MySQL Skill](skills/mysql/SKILL.md)，再打开对应的本地 `references/`。本项目以自托管 MySQL 8.4 和实际执行计划为准，按需参考其中的 InnoDB 通用建议。

Skill 固定到 PlanetScale `database-skills` 提交 [`f6ed002`](https://github.com/planetscale/database-skills/tree/f6ed002d03084cb39c01d1d58904d47a36f5322a/skills/mysql)。文内指向该 Skill 参考文件的链接改为相对路径，以便离线阅读，原始 [MIT 许可证](skills/mysql/LICENSE) 随附。
