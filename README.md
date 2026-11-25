# 同标阅卷微服务平台

同标阅卷面向主观题批量阅卷。平台把评分规则、答卷导入、手写识别、可解释初筛、教师复核、统计导出和通知拆分为独立服务，教师保留最终定分权。前端采用银灰与浅金配色，使用玻璃拟态卡片、金色操作按钮和真实数据仪表盘；强调标签可使用 `#b0c1d3`。

本版本按 2026 年 11 月 12 日作业文档对齐服务名称、接口编号、文件导入、重试与技术组件，并更新金灰配色与原试卷预览。7 个业务服务、28 项功能和 38 项登记接口（原文档 37 项加原件查看）的对应关系见 [contracts.json](docs/contracts.json)，实现边界和验收入口见 [实现与文档对应说明](docs/implementation-alignment.md)。覆盖以下闭环：

```text
登录
  -> 创建阅卷批次
  -> 编辑并冻结评分规则
  -> 导入文本答卷
  -> 异步初筛并返回证据
  -> 教师生成评语草稿并确认最终分
  -> 写入成绩快照与审计记录
  -> 查看漏点统计
  -> 导出 XLSX 或 CSV
```

## 一、服务结构

| 编号 | Maven 模块 | 端口 | 职责 |
|---|---|---:|---|
| S00 | `smartgrade-gateway` | 8080 | 统一入口、JWT 校验、身份头透传、路由 |
| C01 身份与教师偏好服务 | `smartgrade-user-service` | 8081 | BCrypt 登录、JWT、角色与五类教师偏好 |
| C02 评分规则服务 | `smartgrade-rubric-service` | 8082 | 六类要点、100% 权重校验、不可变规则版本 |
| C03 答卷批次服务 | `smartgrade-answer-service` | 8083 | 批次、幂等导入、文件校验、处理进度 |
| C04 可解释初筛服务 | `smartgrade-grading-service` | 8084 | OCR、PDF 文本/扫描页、匹配、证据、最多三次重试 |
| C05 复核定分服务 | `smartgrade-review-service` | 8085 | 三状态复核、评语草稿、人工定分、快照与审计 |
| C06 统计报告服务 | `smartgrade-report-service` | 8086 | 已定分汇总、漏点统计、CSV/XLSX 导出 |
| C07 任务通知服务 | `smartgrade-notification-service` | 8087 | 任务事件、站内通知、SSE 与轮询、已读状态 |
| Common | `smartgrade-common` | - | 统一响应、异常、traceId、JWT、身份上下文 |
| Web | `smartgrade-web` | 5173 | Vue 3 + Vite 教师工作台 |

## 二、快速启动

下载源码交付包后，先阅读根目录的“首次运行与依赖安装.txt”。源码包不包含 JDK、Maven、Node.js、Nacos、Sentinel JAR、node_modules 或 Maven 缓存；依赖由接收者自行安装或在首次构建时联网下载。

### 1. 环境

当前便携环境位于：

| 工具 | 版本 | 路径 |
|---|---|---|
| JDK | Microsoft OpenJDK 17.0.21 LTS | `D:\SmartGradeTools\jdk17` |
| Maven | Apache Maven 3.9.9 | `D:\SmartGradeTools\maven` |
| Node.js | 本机已有版本 | `F:\soft\node.exe` |

启动脚本会通过 `scripts/mvn17.cmd` 自动设置 `JAVA_HOME` 和项目内 Maven 仓库，不修改系统环境变量。

上表是开发者本机路径。其他电脑可设置 `SMARTGRADE_TOOLS`，其下需放置 `jdk17` 和 `maven` 两个目录。Node.js / npm 需加入 PATH。Sentinel 控制台优先读取 `SENTINEL_DASHBOARD_JAR` 指定的绝对路径，其次读取 `SMARTGRADE_TOOLS/sentinel/sentinel-dashboard.jar`；保留原项目上两级的 JAR 路径以兼容开发环境。所有第三方组件均不随源码包分发。

### 2. 一键启动

先按原步骤启动 Nacos，等待控制台 `http://127.0.0.1:8088` 可访问。再双击项目根目录的 `start.cmd`，或在 PowerShell 中执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-all.ps1
```

脚本会执行以下动作：

1. 停止本项目旧服务（包含遗漏进程记录的服务）与 Sentinel，释放 JAR 和端口；清理构建全部 Maven 模块，并把共享动态配置和 Sentinel 规则发布到 Nacos。
2. 启动 Sentinel Dashboard、7 个领域服务、Gateway 和 Vue 前端。
3. 等待 8 个服务的健康端点全部返回 `UP` 且前端可访问，服务随后会向 Sentinel 发送心跳。
4. 打开浏览器访问 `http://127.0.0.1:5173`。

不必另外手动启动 Sentinel。构建日志保存在 `logs/build.log`。同时重复运行启动脚本会被拦截；启动失败会清理已启动的项目进程。默认 local 模式的数据保存在内存，重新启动后演示数据会清空。

演示账号：

| 账号 | 密码 | 角色 |
|---|---|---|
| `teacher1` | `Teach2026` | 教师 |
| `assistant1` | `Teach2026` | 助教 |
| `admin` | `Teach2026` | 管理员 |

停止服务：

```powershell
.\scripts\stop-all.ps1
```

停止脚本会按本项目 JAR、Vite 和 Sentinel 的完整路径补查残留进程，并校验进程名和启动时间，避免 PID 复用后误停其他程序。普通停止保留 Nacos；需要全部关闭时运行 `scripts/stop-all.ps1 -IncludeNacos`，默认 Nacos 目录为当前用户的 `ai-infra/nacos/standalone/nacos-3.3.0`，可用 `-NacosHome` 指定其他目录。

### 3. 构建和测试

```powershell
.\scripts\mvn17.cmd clean verify
cd .\smartgrade-web
npm install
npm run build
```

测试覆盖：

- 登录约束、教师偏好、规则版本和冻结不可变校验。
- 文件类型、ZIP 路径、导入幂等、失败重试与复核投递恢复。
- 关键词、同义表达匹配与证据偏移，以及人工定分和事件通知。
- 每个服务启动时由 Flyway 执行建表及增量迁移。

完整本地业务流程可运行 `scripts/verify-document-contract.ps1` 验证。

## 三、演示步骤

1. 使用 `teacher1 / Teach2026` 登录。
2. 在“阅卷空间”中创建阅卷批次，题号填写 `Q1`，基础分默认 `10`。
3. 修改评分细则，点击“创建并解析”。
4. 核对每个要点的权重与分值，点击“保存并冻结”。
5. 点击上传区域或拖入 `samples/2026101.txt`、`2026102.txt`、`2026103.txt`。
6. 等待进度从“处理中”变为“待复核”。
7. 进入“复核定分”，查看原文证据、卷面覆盖率、字符区间和置信度。
8. 生成两条评语草稿，教师修改后确认最终分。
9. 进入“统计报告”，查看平均分和漏点率，生成 XLSX。
10. 在“教师偏好与任务通知”中维护评语偏好并查看站内通知。

示例答卷位于 `samples/`，文件名会作为学生编号。

## 四、运行模式

### 默认本地演示模式

- 每个服务使用独立的内存 H2 schema。
- 启用 Flyway 建表校验。
- 8 个应用统一注册到 Nacos 公共命名空间。
- Gateway 通过 Nacos 服务发现和 `lb://` 路由访问下游服务。
- 适合课堂演示，重启服务后数据会清空。

Nacos 默认连接信息为 `http://127.0.0.1:8848`、用户名 `nacos`、密码 `1111`。
可通过 `NACOS_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`、`NACOS_NAMESPACE`
和 `NACOS_GROUP` 覆盖。启动后可在 `http://127.0.0.1:8088/` 的“服务管理”中查看
`smartgrade-gateway`、`smartgrade-user-service`、`smartgrade-rubric-service`、
`smartgrade-answer-service`、`smartgrade-grading-service`、
`smartgrade-review-service`、`smartgrade-report-service` 和
`smartgrade-notification-service` 的实例状态。

手工重新发布 Nacos 配置：

```powershell
.\scripts\publish-nacos-config.ps1
```

共享配置 Data ID 为 `smartgrade-common.yaml`。Spring Boot 3 使用
`spring.config.import` 在应用上下文启动阶段加载配置，`@RefreshScope` 负责动态刷新，
不需要为了修改卷面覆盖率阈值或 OCR 模型名称而重新打包或重启。基础分在创建案例时单独保存，随后随规则版本传入初筛，不受全局配置变更影响。

## 五、手写识别与阅卷规则

### 真实手写识别

图片不会伪装成文本成功。`smartgrade-grading-service` 会把 JPG、JPEG 或 PNG 答卷交给
OpenAI 兼容的视觉大模型，要求其只转录手写正文，并同时估计作答区域覆盖率。接口返回
JSON 后，系统再执行确定性的关键词匹配和评分。API 未配置、网络失败或返回格式错误时，
答卷会明确进入 `FAILED`，不会生成假分数。

配置方式：

```powershell
$env:VISION_API_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
$env:VISION_MODEL = "qwen3-vl-plus"
$env:VISION_API_KEY = "你的 API Key"
```

也可以使用其他 OpenAI 兼容视觉模型接口。图片会发送给该外部模型服务，正式处理真实学生
答卷前应取得数据使用授权。无需另外安装 OCR 软件；必须准备一个支持图片输入的模型 API Key。

### 五四论述题评分

页面已完整内置“如何争做新时代好青年，早日入党”的五四论述题题干。题号默认为 `Q1`，可填写1–20位字母、数字、下划线或短横线。创建案例时可同时设置满分和基础分，基础分默认10，须在0到满分之间。默认满分20、基础分10时，评分口径为：

```text
有效作答基础分 10
+ 有理想        2.25
+ 敢担当        2.25
+ 能吃苦        2.25
+ 肯奋斗        2.25
+ 卷面覆盖率达标 1.00
----------------------
最高分          20
```

没有作答为 0；有效作答从该案例保存的基础分起算，其余分数分配给要点。例如基础分改为8时，要点合计为12分。图片的覆盖率由视觉模型估计“手写内容
占可用作答区域的比例”，默认要求至少 50%；文本示例没有纸张图像，因此以 240 个有效
字符作为可复现的篇幅回退口径。关键词和篇幅得分相加后按四舍五入保留整数。

## 六、Nacos、Sentinel 与 Gateway

- 服务注册：8 个 Spring Boot 应用注册到 `public / DEFAULT_GROUP`。
- Feign 调用：客户端只声明 `@FeignClient(name = "smartgrade-xxx-service")`，由 Spring Cloud LoadBalancer 完成发现与选址。
- 动态配置：`smartgrade-common.yaml` 集中提供评分阈值和 Sentinel 数据源配置，修改后通过 `@RefreshScope` 刷新。
- 流量控制：`answer-import` 资源默认限制为 5 QPS；超限返回 HTTP 429，不会继续写数据库。
- 熔断降级：评分与规则 Feign 客户端提供 `FallbackFactory`，下游不可用时返回明确的服务降级原因。
- 网关：Spring Cloud Gateway 是唯一外部入口，负责 `lb://` 路由、JWT 鉴权和请求身份头透传；Sentinel Gateway 适配器负责统一限流与 JSON 降级响应。

一键启动会自动运行 Sentinel 控制台；也可以单独启动：

```powershell
.\scripts\start-sentinel-dashboard.ps1
```

打开 `http://127.0.0.1:8858`，演示用户名和密码均为 `sentinel`。

完整运行态验收（会直接读取 Nacos、Sentinel 和 Gateway 的实时接口）：

```powershell
.\scripts\verify-runtime.ps1
```

课堂演示 Sentinel 网关限流时，先在控制台打开 `smartgrade-gateway` 的“实时监控”或
“网关流控”，再运行：

```powershell
.\scripts\demo-sentinel-limit.ps1
```

脚本会并发请求真实的 `/api/batches` 路由，输出实际通过数量与 HTTP 429 数量；429 响应
来自 Gateway 的 Sentinel 阻断处理器，并非前端模拟数据。Gateway 的所有响应还会携带
`X-SmartGrade-Gateway: smartgrade-gateway`，可在浏览器网络面板中直接核验。

### Docker 基础设施模式

`deploy/docker-compose-infra.yml` 提供：

- MySQL 8.4
- Nacos 2.3.2
- Redis 7.4
- Kafka 3.7
- MinIO

启动实例：

```powershell
docker compose -f .\deploy\docker-compose-infra.yml up -d
```

随后各服务以 `infra` profile 启动。数据库、用户名和 Nacos 地址均通过环境变量配置，示例见 `.env.example`。当前开发机未安装 Docker Desktop，因此该模式保留了完整配置，但本机默认验收使用内存 H2。

## 七、统一约定

- 前端请求统一进入 Gateway。
- 普通接口返回 `ApiResponse<T>`：`code`、`message`、`data`、`traceId`、`timestamp`。
- Controller 只处理 HTTP 契约，不包含 `if`、`switch`、循环或 Repository 调用。
- 请求和响应使用 DTO，并启用 Jakarta Validation。
- Feign Client 配置连接超时、读取超时和 traceId 透传。
- 审计、快照、最终分和评语确认在复核服务的短事务内写入。
- 每个服务只写入自己的 schema。

## 八、当前能力边界

- 文本答卷可完整跑通规则初筛；JPG、JPEG、PNG 图片在配置视觉模型后完成手写转录。上传保留原始字节，复核页可查看原试卷、缩放图片、浏览 PDF / TXT 并下载原文件；ZIP 按解压后的单份答卷查看。历史文件保留当时上传的版本。
- 图片及扫描 PDF 识别需要配置视觉模型 API Key；未配置或识别失败时按重试上限进入人工复核。文字 PDF 通过 PDFBox 提取。ZIP 展开为 TXT、PNG、JPG、JPEG 或 PDF 答卷，展开后仍最多 20 份；禁止嵌套 ZIP。
- 默认演示模式使用内存数据库，适合闭环验证；生产或正式验收应启用 MySQL 基础设施模式。
- `infra` 模式实际使用 Kafka 消息、Redis 幂等/执行互斥和 MinIO 原文件/导出文件；默认本地模式通过 Spring 事件、本地文件及内存适配器运行同一业务链路。
- 初版个性化评语使用教师偏好加证据的模板生成，后续可替换为大模型适配器。

## 九、项目目录

```text
smartgrade-platform/
├── pom.xml
├── smartgrade-common/
├── smartgrade-gateway/
├── smartgrade-user-service/
├── smartgrade-rubric-service/
├── smartgrade-answer-service/
├── smartgrade-grading-service/
├── smartgrade-review-service/
├── smartgrade-report-service/
├── smartgrade-notification-service/
├── smartgrade-web/
├── deploy/
├── samples/
├── scripts/
├── start.cmd
└── stop.cmd
```
