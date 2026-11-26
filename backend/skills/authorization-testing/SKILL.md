---
name: authorization-testing
description: 为 Spring Boot API 编写权限回归。适用于修改 Spring Security、角色分配、检测任务归属、历史列表或结果与媒体访问时。
---

# 后端权限回归

以[检测接口契约](../../../contracts/detection.md)中的权限矩阵和状态码为准。先区分已实现的样本编号接口 `/api/detections` 与文件接口草案 `/api/detection-tasks`，按本次涉及的接口选择测试。

## 测试矩阵

| 场景 | 断言 |
| --- | --- |
| 未认证 | 访问受保护 API 返回 `401`。公开健康检查仍可访问。 |
| 角色不足 | `VIEWER` 提交检测返回 `403`。普通用户访问管理员角色接口返回 `403`。有副作用的拒绝请求还要检查任务或角色状态。 |
| 角色授权 | `OPERATOR` 和管理员可提交任务。管理员修改角色后，使用被修改账号再次请求，验证新权限生效。 |
| 数据归属 | 准备两个不同用户及其任务，至少用一组同角色账号覆盖列表隔离。验证当前用户只看到自己的任务，管理员按契约可看全部。 |
| 文件任务草案实现后 | 分别覆盖任务详情、结果及媒体访问的归属校验。任务详情和结果的越权与不存在按草案返回 `404`。历史列表先限制可见范围再分页，`items` 和 `total` 都应反映权限范围。 |

## 选测试层

- 使用带实际安全配置的 MockMvc 测试验证认证、授权和 HTTP 状态。当前演示使用 HTTP Basic。角色修改后的行为应走实际身份解析和角色查询路径。
- 测试任务归属时保留真实可见范围查询。当前可用 `InMemoryDetectionRepository` 放入夹具，使用独立用户和任务，隔离算法传输。新增持久层后按所选数据库补集成测试。
- 构造有效请求来验证拒绝路径，避免把请求格式错误当作权限拒绝。权限变更涉及多个身份时，在同一用例中明确每次请求的调用者及预期。

Spring Boot 4 的测试配置参考[随附的 Piomin 测试章节](../claude-ai-spring-boot/skills/spring-boot/references/testing.md)和 [Spring Security MockMvc](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/index.html)。模板的 `AccountAccessTest` 使用真实 HTTP 请求验证管理员接口。在 `backend/` 执行 `mvn -Dtest=AccountAccessTest test` 或 `mvn verify`，核对测试数。
