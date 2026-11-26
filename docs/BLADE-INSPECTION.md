# 风机叶片巡检平台

巡检人员登录后打开「叶片检测」，填写风场和风机编号，提交一个图片 ZIP。上传中断后可重新提交。后台使用一个工作进程依次处理，完成后发布在线报告和 PDF。管理员从「巡检账号」创建操作员。统计采用 `blade_defect_v1.1.onnx` 的 DQ、WR、TL、LW、KL、LJ、LJZS、YJPS、FYZTL、ZGTL 十类。缺陷数量按检测框计数。在线报告保留逐图明细，PDF 展示同一份分类统计和最多六张代表性检出图。

## 准备 Ubuntu 服务器

准备 Docker Engine、Compose 和 NVIDIA GPU 容器运行环境。用 `nvidia-smi` 核实驱动，记录 `/etc/os-release`。准备空间充足的数据盘。数据目录要包含 `uploads` 和 `reports`，容器用 UID/GID `10001:10001`。将交付的 `blade_defect_v1.1.onnx` 放到模型目录并允许容器读取。原始 ZIP、报告和数据库按部署方的留存规则备份与回收。

```bash
sudo install -d -o 10001 -g 10001 -m 0750 /srv/blade/data/uploads /srv/blade/data/reports
sudo install -d -m 0755 /srv/blade/models
sudo install -m 0644 /path/to/blade_defect_v1.1.onnx /srv/blade/models/blade_defect_v1.1.onnx
```

从 `.env.example` 复制巡检变量到 `.env.blade`。将 `BLADE_DATA_DIR` 与 `BLADE_MODEL_DIR` 设为上述路径，设置独立的数据库密码和初始 `admin` 密码。管理员密码至少 12 个字符，UTF-8 编码不超过 72 字节。已有实例的管理员密码不会因重启而覆盖。默认只在 `127.0.0.1:8090` 监听，由内网 HTTPS 反向代理统一暴露。配置经过其他代理时也要允许流式转发 10 GB 原始请求体。

```bash
docker compose --env-file .env.blade -f compose.blade.yaml -f compose.blade.gpu.yaml config --quiet
docker compose --env-file .env.blade -f compose.blade.yaml -f compose.blade.gpu.yaml up -d --build --wait
docker compose --env-file .env.blade -f compose.blade.yaml -f compose.blade.gpu.yaml exec algorithm \
  python -c 'from urllib.request import urlopen; print(urlopen("http://127.0.0.1:8000/health").read().decode())'
```

算法探针应显示 `CUDAExecutionProvider`。开发冒烟可只用 `compose.blade.yaml` 运行 CPU。巡检报告按原始图片切出重叠区域推理，对跨切块重复框去重。上线前用实际整图和标注核查检测效果，根据现场样本确认阈值和验收结果。

## 验收

1. 用管理员创建两个巡检账号。用第一个账号上传一个混有含缺陷与无检出照片的实际 ZIP。
2. 等待完成并打开标注预览，下载 PDF，核对任务编号、各类与总数一致。
3. 用第二个账号打开第一位巡检员的任务、预览和 PDF，确认均拒绝。用管理员再次打开，确认能够访问。
4. 上传坏 ZIP 和超限 ZIP，核对失败提示。验收机用接近 10 GB 的实际文件确认代理与数据盘容量。

图片流程接受 JPEG 与 PNG。默认限制为 ZIP 解压后 30 GB、十万条目、单图 100 MB 和五千万像素。输入文件名保留在报告，不作为服务器文件路径。原件保存在上传目录。

### 浏览器回归

将 `E2E_BASE_URL` 指向要验证的巡检部署，将 `BLADE_E2E_ZIP` 指向本地小型验收 ZIP，并将测试实例的初始管理员密码设为当前进程的 `BLADE_ADMIN_PASSWORD`。已安装 Edge 时可设置 `E2E_CHROME_CHANNEL=msedge`。测试会创建两个巡检账号并保留一份测试任务，请使用隔离的验收实例。

```powershell
cd frontend/vue
npm.cmd ci
npm.cmd run test:e2e -- tests/blade-inspection.spec.ts --trace off
```

确认输出为 `1 passed`。缺少必需的环境变量时，用例会跳过。
