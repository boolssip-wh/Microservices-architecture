# 基础设施运行说明

应用以 `infra` profile 启动时，真实使用 MySQL、MinIO、Redis 和 Kafka。本地演示使用 H2、本地文件和进程内幂等存储，通知提交后通过内部 HTTP 接口投递。

在项目目录执行 `docker compose -f deploy/docker-compose-infra.yml up -d`，再将应用进程环境变量 `SPRING_PROFILES_ACTIVE` 设为 `infra`。各服务的 `application-infra.yml` 使用独立 `USER_DB_URL`、`RUBRIC_DB_URL`、`ANSWER_DB_URL`、`GRADING_DB_URL`、`REVIEW_DB_URL`、`REPORT_DB_URL`、`NOTIFICATION_DB_URL`，默认分别连接七个 `smartgrade_*` schema。Java 进程不会自动读取 `.env.example`，需通过启动环境传入所需值。

| 组件 | 使用位置 | 验证方式 |
| --- | --- | --- |
| MySQL 8.4 | 七服务独立 schema，Flyway 升级 | 查看各服务启动日志中的迁移和 JPA 校验 |
| Redis 7.4 | `RedisIdempotencyStore` 的原子 SET NX 与过期时间 | 导入任务幂等请求返回同一 jobId，Redis 可见 `smartgrade:*` 键 |
| MinIO | 原始答卷和 CSV/XLSX 导出文件 | 查看 `smartgrade` 桶中的对象，业务表只保存 key |
| Kafka 3.7.2 | `grading.requested` 由 C04 消费，再委托 C03 幂等推进批次；五类完成/失败事件由 C07 消费 | 查看主题与消费组；通知按 eventId 去重 |
| Nginx | 静态前端、`/api/` 反向代理到 Gateway | 通过 8090 端口访问应用，SSE 禁用代理缓冲 |

MinIO 桶在首次写入时创建。Kafka 主题通过 `KafkaAdmin.NewTopics` 创建。七个业务应用使用相同的 `MINIO_BUCKET` 和 `SERVICE_TOKEN`；所有内部 HTTP 请求均验证 `X-Service-Token`。本机调用 Kafka 使用 `127.0.0.1:9092`，容器网络内使用 `kafka:9094`。

前端构建完成后，执行 `docker compose -f deploy/docker-compose-infra.yml --profile web up -d nginx`，使用 `http://127.0.0.1:8090` 访问。Gateway 及业务 Java 服务运行在主机上，Nginx 经 `host.docker.internal:8080` 转发。Nginx 配置支持 Vue Router history 回退和通知 SSE。

通知在业务事务提交后发布，投递失败会记录带事件编号的错误日志，不回滚已经完成的业务事务。当前未实现事务发件箱，故障场景下需要运维重放失败事件。Kafka 消费端以事件编号唯一索引避免重复生成通知。

Kafka 3.7.2 官方镜像版本依据：[Apache Kafka 3.7 Docker 文档](https://kafka.apache.org/37/getting-started/docker/)。
