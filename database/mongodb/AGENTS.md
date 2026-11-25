# MongoDB

配置位于此目录的 `compose.yaml`。从项目根目录执行 `docker compose up -d mongodb`。使用 `docker compose ps mongodb` 检查启动态和认证后的 ping。

按照文档的读写模式选择索引及更新边界。接入业务后创建应用账号。初始化账号参数只在空数据卷首次启动时应用。
