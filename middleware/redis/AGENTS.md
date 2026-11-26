# Redis

用于新项目的缓存或有时效的数据。选用时复制本目录，配置 `REDIS_PORT`（默认 `6379`）。配置使用 AOF 和 `redis_data` 命名卷，主机端口只绑定本机回环地址。

从仓库根目录运行 `docker compose up -d redis`，再用 `docker compose exec redis redis-cli ping` 检查是否返回 `PONG`。独立取用时，在模块目录执行相同命令。已有数据卷的项目沿用原 Compose 项目名。
