# MySQL

配置位于此目录的 `compose.yaml`。从项目根目录运行 `docker compose up -d mysql`，再用 `docker compose ps mysql` 查看健康状态。

- 使用 MySQL 8.4 验证 SQL，明确字符集、排序规则和索引长度等取值。运行时使用 `MYSQL_USER` 对应的应用账号。
- 初始化账号和库依赖首次启动时的空数据卷。已有数据库的账号变更使用显式 SQL 和对应的应用配置更新。
