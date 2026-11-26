# AI 检测示例

当前演示通过样本编号连接前端、Spring Boot 和算法。已实现接口见[接口契约](../../contracts/detection.md)。ZIP 和视频上传、历史检索、备份和恢复等后续设计分别见[业务草案](design.md)和[页面草案](pages.md)。

## 在新项目中取出空白后台

- **前端**。在 Vue 的 `src/router/sections.ts` 或 React 的 `src/app/sections.ts` 将 `modules` 初始值改为 `[]`。移除 `src/features/detection/` 及 `src/views/DetectionsView.vue` 或 `src/pages/DetectionsPage.tsx`。保留 `tests/admin-shell.spec.ts` 和 `tests/demo-api.ts` 中的账号辅助函数，移除或替换 `tests/workbench.spec.ts` 中的检测场景。
- **后端**。移除 `DetectionController`、`DetectionService`、`InMemoryDetectionRepository`、检测 DTO、检测 model 及 `integration/detection/`。在 `SecurityConfig` 删除检测路由规则，保留 `/api/me`、`/api/admin/**` 及默认拒绝规则。清理不用的检测配置和 Kafka、REST 客户端依赖，保留账号 Repository、角色接口和健康检查。
- **验证**。在选中前端运行 `npm run build` 和 `npm run test:e2e -- admin-shell.spec.ts`。在 `backend/` 运行 `mvn verify`。浏览器用例覆盖登录、工作台、角色管理和退出，服务端权限用例覆盖同一管理接口的三种身份。

数据库、CI 和 Compose 的裁剪步骤见[新项目取用清单](../../README.md#新项目取用清单)。
