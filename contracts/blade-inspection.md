# 叶片巡检文件接口

启用 `postgres,blade` profile 后提供接口。同源 `/api/` 路径沿用模板的 HTTP Basic 认证。写入请求需要 `X-Requested-With: XMLHttpRequest`。巡检员工用 OPERATOR，管理员可读所有任务。历史任务按账号 ID 隔离，跨账号访问返回 404。

| 方法和路径 | 输入和结果 |
| --- | --- |
| `POST /api/detection-tasks` | `OPERATOR` 或 `ADMIN`。查询参数为 `farmName`、`turbineNumber`、`filename`，正文是 `application/zip` 的原始 ZIP 字节。成功返回 202、任务摘要及 Location。ZIP 格式无效或无可处理图片返回 400，超过 10,000,000,000 字节返回 413。断线后用户重新提交。 |
| `GET /api/detection-tasks` | `page` 和 `size` 默认为 1 和 20。返回 `{items, page, hasNext}`，按提交时间倒序。 |
| `GET /api/detection-tasks/{id}` | 返回 `{id, farmName, turbineNumber, originalName, status, createdAt, failureMessage}`。状态为 `PENDING`、`RUNNING`、`COMPLETED`、`FAILED`。 |
| `GET /api/detection-tasks/{id}/report` | 返回任务和模型信息、处理图数 `imageCount`、检出图数 `defectImageCount`、检出总数 `detectionCount` 和十类缺陷计数 `classes`。 |
| `GET /api/detection-tasks/{id}/images` | 同样分页，每项包含从零开始的 `index`、ZIP 内文件名、图像宽高和 `detections`，每个检测包含类别、分数和四点坐标。 |
| `GET /api/detection-tasks/{id}/images/{index}/preview` | `image/jpeg`，继承任务访问范围。 |
| `GET /api/detection-tasks/{id}/report.pdf` | `application/pdf`，以附件返回。 |
| `POST /api/admin/blade-operators` | `ADMIN` 发送 `{username,password}` 创建巡检账号。成功 201，重复 409。 |

任务尚未有报告时，报告、明细、预览和 PDF 请求返回 409。任务 ID 存在性和归属校验优先于报告状态。解码错误或 ZIP 成员损坏会在后台把任务标记为失败。损坏成员不会发布部分报告。

算法服务只在内部网络开放。`POST /blade/inspect` 接收 `taskId`、`attemptId`、`farmName`、`turbineNumber`、`originalName`、`createdAt`，完成文件写入后返回 204。算法从共享数据目录读取 `uploads/{taskId}.zip`，按执行 ID 原子发布 `reports/{attemptId}`。其中 `summary.json` 是在线和 PDF 报告共用的统计来源。后端仅开放数据表所指向的已完成执行。启动时把中断的 `RUNNING` 任务放回队列，以新执行 ID 处理。
