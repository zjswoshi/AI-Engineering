# MinIO 社区版兼容测试

此目录供已有 MinIO 社区版项目在本机复现 S3 接口行为。官方于 2026 年 4 月 25 日归档了社区版服务端仓库。选型时请区分社区版与使用独立许可的 AIStor。[社区版仓库](https://github.com/minio/minio) · [AIStor 文档](https://docs.min.io/aistor/)

在本目录将 `.env.example` 复制为 `.env`，填写待验证的镜像引用，可用摘要复现同一版本，设置本地管理员和至少 8 个字符的随机密码。执行以下命令校验配置、启动实例并检查 S3 服务就绪状态。端口经过调整时使用实际的 `MINIO_API_PORT`。

```powershell
docker compose config --quiet
docker compose up -d minio
curl.exe --fail http://127.0.0.1:9000/minio/health/ready
```

默认 S3 API 端口为 `9000`，控制台端口为 `9001`。使用 S3 客户端创建私有测试桶，给应用分配限定桶权限的访问密钥。后端和算法通过 S3 endpoint、bucket 和 object key 读取同一份文件。管理员凭证只用于管理实例。
