# Kafka

`compose.yaml` 提供单节点 KRaft 开发服务，容器内部地址为 `kafka:9092`。

- 调整消息前阅读 `contracts/detection.md`，检查生产者与两个消费者的数据格式和处理顺序。
- `kafka_data` 命名卷保存 Kafka 消息、KRaft 元数据与消费位点。当前配置用于单机开发。实际部署按环境配置认证、授权、复制和持久化。
- 验证时提交一个任务，确认返回 `202`，再查询到相同任务 ID 的结果。
