# 风机叶片巡检平台离线部署

部署包适用于装有 Docker Engine、Compose 和 NVIDIA Container Toolkit 的 Ubuntu `x86_64` 服务器。运行 `nvidia-smi` 核对宿主机驱动。数据盘需预留空间给解包后的镜像归档、Docker 导入的镜像、上传的 ZIP、数据库和检测报告。

将压缩包传到服务器，在其所在目录执行以下命令。将包名换成实际文件名。

```bash
tar -xzf blade-inspection-<版本>-linux-amd64.tar.gz
cd blade-inspection-<版本>-linux-amd64
sudo bash deploy.sh
```

脚本导入四个镜像并等待服务启动。算法探针应显示 `CUDAExecutionProvider`。首次启动会生成数据库密码和 `admin` 初始密码，保存在 `/srv/blade/.env.blade`。用 `sudoedit /srv/blade/.env.blade` 查看或调整监听配置。重跑脚本会沿用同一份配置和数据。

网页默认监听 `127.0.0.1:8090`。在服务器配置内网 HTTPS 反向代理，将请求转发到这个地址，并允许流式转发最大 10 GB 的上传请求。将真实 ZIP 提交为一个检测任务，待完成后打开逐图标注并下载 PDF。

上传原件和报告保存在 `/srv/blade/data`，PostgreSQL 使用 Docker 命名卷 `blade-inspection_blade_db`。升级时将新包解压到独立目录，再运行其中的 `deploy.sh`，保持同一个 Docker Compose 项目名及 `/srv/blade`。升级前备份该目录和数据库。可用 `BLADE_STATE_DIR` 指定绝对路径的数据及配置目录，单机运行多个实例时需各自设置独立的 `COMPOSE_PROJECT_NAME` 和监听端口。
