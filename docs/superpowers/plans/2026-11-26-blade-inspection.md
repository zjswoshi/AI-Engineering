# 风机叶片巡检平台首版

工作区为 `E:\zhiying\zjs\uav`，起点为干净的 `project/uav`。2026 年 11 月 26 日确认实施。

- [x] 真实 ZIP 提交。Spring Boot 按账号记录任务，原件流式落盘。Flyway 增加巡检任务迁移。用超限、非法 ZIP 和跨账号请求验证边界。
- [x] 检测和报告。单个 Python 工作进程按任务处理图片，用 `blade_defect_v1.1.onnx` 十类口径生成统一统计、逐图记录、标注预览和中文 PDF。用真实巡检图核查模型运行和报告产物。
- [x] 浏览器流程。Vue 工作台提供上传和任务列表，任务详情展示进度、报告和预览。执行前端构建和浏览器冒烟。
- [x] 单机交付。Compose 连接 PostgreSQL、Spring Boot、Vue/Nginx 和算法服务，模型和数据通过外部目录提供。验证 Compose 配置、实际可用环境及目标机待验证条件。

上传上限是 10,000,000,000 字节。每份 ZIP 对应一台风机。上传中断后重新上传。巡检人员仅能访问自己的任务，管理员可访问全部。报告自动发布。

## 验证记录

- 2026 年 11 月 26 日，本机 Docker 中用 NVIDIA GeForce RTX 3060 运行算法，健康探针返回 `CUDAExecutionProvider`。从 Vue/Nginx 入口运行 `playwright test tests/blade-inspection.spec.ts --trace off`，1 项通过。
- 真实 ZIP 含 12 张图片。GPU 生成 12 张预览，报告有 1 张检出图片和 3 个框，`WR=2`、`LW=1`。将两页 PDF 渲染为图片后，核对中文、十类统计和代表性标注图。
- 已通过坏 ZIP 的 `400`、跨账号任务和产物的 `404`、管理员的 `200`。此前本机 Nginx 收到 9,995,779,838 字节的合成 ZIP64 后返回 `202` 并生成报告，声明 10,000,000,001 字节的请求返回 `413`。
- 本机 `npm.cmd ci` 和 `npm.cmd run build` 通过。默认 Playwright 回归 4 项通过，巡检专用用例在 GPU 实例单独运行通过。`docker build --no-cache-filter build --target build --output type=cacheonly ./backend` 复跑 Maven `verify`，3 项通过。CPU/GPU Compose 配置与 `nginx -t` 通过。
- [ ] 在目标 Ubuntu 服务器核实实际系统版本、GPU 和内网 HTTPS，用业务 ZIP 验证上传容量及报告交付，使用带标注的样本评估整图检测效果。需要目标机访问方式及现场验收样本。
