# Nginx

`default.conf` 由前端镜像加载，使用 Docker 容器网络解析 `backend`。

- `/api/` 原样转发路径到 Spring Boot，静态资源和 SPA 导航分别由各自的 `location` 处理。
- 修改后在容器中执行 `nginx -t`，然后检查首页、SPA 深链接和 `/api/health`。不存在的 API 或静态资源应返回对应错误状态。
