# Docker

根目录 `compose.yaml` 是组合入口。`frontend.Dockerfile` 用 `APP_DIR` 构建 `vue` 或 `react`，由 Nginx 提供静态文件。后端镜像定义在 `backend/Dockerfile`。

- 前端镜像使用已提交的 npm 锁文件。构建上下文排除本地环境文件、依赖和产物。
- 默认将开发端口绑定到回环地址。可选服务通过 Compose profiles 启动。
- 修改后运行 `docker compose --profile '*' config --quiet`。有 Docker Engine 时构建受影响镜像并检查对应服务实际启动情况。
