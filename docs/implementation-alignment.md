# 实现与文档对应说明

本目录的 `contracts.json` 记录 2026 年 11 月 12 日作业版本的 7 个服务、28 项功能、38 项登记接口：原 API 文档 37 项，加本次 `C03-4-03` 原件查看接口。中文表名中的“表”是 ER 图展示名称，数据库保留 `teacher`、`batch`、`grading_task` 等英文标识，便于既有数据和 SQL 迁移兼容。

## 服务与技术

| 范围 | 实现 |
| --- | --- |
| 表现层 | Vue 3.5、Vue Router、Vite 6、Fetch、File API；沿用现有 CSS 与动画 |
| 业务框架 | Java 17、Spring Boot 3.2.9、Spring Cloud 2023.0.3、Alibaba 2023.0.3.2、Validation、JPA、Flyway |
| 安全与调用 | BCrypt、JWT 0.12.6、Gateway、OpenFeign、LoadBalancer、内部服务令牌 |
| 服务治理 | Nacos 2.3.2 注册/配置、Sentinel 限流/降级、Actuator、OpenAPI |
| 数据与基础设施 | infra 启用 MySQL 8.4 独立 schema、Redis 7.4、Kafka 3.7、MinIO，Nginx 转发 Gateway |
| 导出与识别 | Apache POI 导出 XLSX、CSV；PDFBox 提取 PDF 文字或渲染扫描页；视觉模型只作转录 |

Spring Kafka 使用 Spring Boot 管理的配套客户端版本；Kafka 3.7 指服务端版本。参考 [Spring 官方兼容说明](https://spring.io/projects/spring-kafka/) 与 [PDFBox 3 加载接口说明](https://pdfbox.apache.org/3.0/migration.html)。

## 数据边界

C04 保存 `grading_task`、`grading_result` 和 `hit_evidence`。C05 保存复核记录、评语草稿、成绩快照与审计，并通过内部 API 接收独立的 `review_result_projection`/`review_evidence_projection` 读模型，以支持复核页面及 C06 汇总。读模型不是跨库连接，也不改变 C04 的结果源数据归属。新增表和字段通过后续 Flyway 迁移应用，不修改已发布 V1 的校验和。

导入按批次和 `Idempotency-Key` 建唯一索引并锁定批次；重复请求返回原 jobId。ZIP 解压后校验数量、总字节和路径，文件以原始字节写对象存储；文本提取结果属于处理数据。学号与 SHA-256 内容指纹同时用于批次内查重。

## 本地与基础设施模式

默认 local 模式使用 H2、Spring 事务提交后事件、本地对象文件和内存幂等存储，便于无 Docker 演示。infra 模式启用真实 MySQL、Redis、Kafka 与 MinIO；七服务数据库地址分别使用 `USER_DB_URL`、`RUBRIC_DB_URL`、`ANSWER_DB_URL`、`GRADING_DB_URL`、`REVIEW_DB_URL`、`REPORT_DB_URL`、`NOTIFICATION_DB_URL`。

`grading.requested` 传递导入任务标识，由 C04 消费，再通过内部接口委托 C03 推进持久化的批次编排；OCR、匹配与初筛结果仍由 C04 执行和保存。派发前已有导入任务持久化记录；发送失败后扫描 PENDING 任务补发。通知使用 grading.completed、grading.failed、report.completed、report.failed、review.confirmed 事件，通知失败不回滚人工最终分。业务幂等和通知 eventId 去重分别承担各自职责。

## 验收

界面修订后，创建案例的题号默认 `Q1`，基础分默认10且可输入；基础分随批次持久化，并贯通规则版本、初筛计算和复核详情。要点分合计必须等于满分减基础分。登录页采用金灰配色及等高两栏，阅卷空间顶部整合宣传语、新建入口和教师信息。

运行 `scripts/mvn17.cmd verify` 与前端 `npm run build`。`scripts/verify-document-contract.ps1` 在 18100–18107 隔离端口启动全部服务，执行登录、规则冻结、幂等导入、初筛、复核定分、导出和通知检查，结束后停止它创建的进程，日志保留于 `.runtime/contract-*`。

应用正在运行时，可用 `-Dsmartgrade.build.directory=target-qa` 构建到独立目录，验收脚本传 `-BuildDirectory target-qa`。默认仍输出 `target`，不改变 `start.cmd` 的使用方式。验收脚本的 `-KeepRunning` 仅用于浏览器调试，会在日志目录写入专属进程记录，调试后须停止这些记录匹配的进程。

2026-11-12 界面与基础分修订后实测：Maven `verify` 的 30 项后端测试全部通过；前端生产构建通过，上传分组幂等回归测试 1 项通过。隔离端口联调的 13 项断言全部通过，覆盖八应用健康及 Flyway 迁移、草稿批次、自定义基础分持久化及初筛/复核传递、版本 1/2 冻结、重复导入、异步初筛、复核完成同步、小数分保留、XLSX 生成/下载及事件通知。服务日志位于 `.runtime/contract-7165320f8bf243d5aa26dcdb4af85c0a`，Maven 日志位于 `.runtime/verify-ui-update.log`。

浏览器在隔离端口 4173 → 18100 实测默认题号 `Q1`、基础分 8 的创建案例、解析要点和保存冻结流程，页面显示 `8 + 10.8 + 1.2 = 20`，浏览器无警告或错误。另核验 1440×960 登录页两栏等高、图标无旋转、金色按钮，以及 390×844 窄屏无横向溢出；截图保留于 `.runtime/ui-previews`。

随后修复 Windows 启动时残留进程占用 JAR、导致 `repackage` 无法重命名文件的问题。停止脚本按应用路径补查遗漏的进程并校验启动时间；启动前自动清理旧实例、执行 `clean package`，保留 `logs/build.log`，逐个记录新进程并在启动失败时清理。完整 Windows PowerShell 启动流程实测通过：11 个 Maven 模块构建成功，8 个后端健康检查及前端访问正常，经 8080 网关登录并创建 `Q1`、基础分 8 的案例成功，同时启动拦截也通过。验证后已关闭应用、Sentinel 和 Nacos，并确认 5173、8080–8088、8848、8858、9848、9849 全部释放。启动、并发保护、退出检查记录分别保留于 `.runtime/startup-repair-validation.log`、`.runtime/startup-lock-validation.log`、`.runtime/startup-cleanup-validation.log`。

本次完整业务验证使用本地模式；本机没有 Docker 命令，未启动 MySQL/Redis/Kafka/MinIO 容器做基础设施模式联调。容器部署说明与当前事件投递的限制见 `deploy/INFRA.md`。38 项登记接口之外，保留当前规则查询、导出下载、内部任务重试/派发、复核状态同步和本地事件接收等辅助接口。

## 原始试卷与识别轨迹修订

识别轨迹的标题、学号、序号、类型、状态、覆盖率和操作文字统一放大。复核页新增 `OriginalAnswerViewer`，使用带 JWT 的 Fetch 读取 `GET /api/batches/{batchId}/answers/{answerId}/original`，支持 PNG/JPEG 缩放、PDF/TXT 预览和原文件下载。切换答卷会取消旧请求并释放 Blob URL。C03 先验证批次归属、答卷归属，再从 `ObjectStorage` 读出原始字节；成功响应为二进制，错误仍为统一 JSON，并设置私有禁止缓存响应头。不新增数据库字段。

前端取消上传图片压缩，文件接受类型与后端统一为 TXT、JPG/JPEG、PNG、PDF、ZIP。ZIP 原件指解压后的对应答卷，历史文件只能提供原先保存的版本。新增 8 项原件测试后，Maven verify 共 38 项通过；前端构建及上传幂等测试通过；完整隔离流程 15 项断言通过，包含原文件逐字节一致和其他教师访问被拒绝。日志见 `.runtime/verify-original-file.log`、`.runtime/verify-original-contract.log`。原有技术图中的 Canvas 上传压缩已不再使用；本机 Nacos 服务端为 3.3.0，与图中的 2.3.2 客户端/容器配置分别说明。

浏览器另核验 TXT 原文显示、文件下载、切换答卷后旧原件清空，以及人工复核中的 JPEG 原图加载和 125% 缩放。JPEG 下载文件的 SHA-256 与上传样例一致，浏览器未出现警告或错误。测试后已停止本次专属的 18100–18107 服务及 4173 前端。配套的《项目结构说明与自定义项目的编码规范》（11 页）与《同标阅卷初代产品使用说明书》（10 页）保存在课程目录的 `更新1101作业文档` 中，原参考文件保留。

文档中的 3 秒登录、单份 30 秒初筛、90% 匹配一致率、127 行 30 秒导出等属于验收目标，需要固定设备、规则与标注样本实测；单元测试和一次功能闭环不构成这些性能或准确率指标的证明。外部视觉模型受网络与提供方延迟影响，未配置时进入明确失败及人工处理路径。
