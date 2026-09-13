# CampusAI Market 新会话交接说明

> 新 Codex 会话请先阅读本文件与 `CONTEXT.md`，再开始修改。
> 创建时间：2026-09-06
> 最后更新：2026-09-11（Phase 14 最终优化已实现，GitHub 远程推送待用户授权）

## 1. 项目一句话现状

CampusAI Market 是一个面向校园二手交易与 Java 后端面试展示的项目。当前已完成后端业务、图片上传、AI Tool Calling、服务端多轮会话、RAG、Vue 3 前端、Docker Compose 完整栈和前后端生产构建优化；仅剩 GitHub 远程仓库创建与推送。

## 2. 当前完成范围

### 后端（已完成）

- 用户：注册、登录、JWT、`/me`、登出 Redis 黑名单。
- 商品：发布、搜索、排序、热门/搜索缓存、详情、修改、下架、重新上架、卖家全状态商品管理。
- 图片：本地上传、格式/大小/文件头校验、公开访问、Docker 数据卷持久化，兼容 URL 图片。
- 收藏：添加、取消、我的收藏、是否已收藏检查。
- 订单：创建订单防并发超卖、买家/卖家订单查询，以及 `CREATED -> CONFIRMED -> IN_PROGRESS -> COMPLETED` 状态机。
- AI Agent：DeepSeek Tool Calling，工具包括 `searchItems`、`getItemDetail`、`getMyOrders`、`recommendItems`；支持服务端会话、多轮记忆和用户隔离。
- RAG：本地 `all-MiniLM-L6-v2` + `SimpleVectorStore`，回答平台交易规则。
- 为前端补充的公开接口：`GET /api/categories`、`GET /api/favorites/check/{itemId}`。

### 前端（Phase 11 + Phase 14 已完成）

- Vue 3 + Vite 6 + Vue Router + Element Plus + Axios + lucide。
- 页面：登录/注册、首页搜索、商品详情、发布/编辑、我的商品、我的收藏、我的订单、三栏 AI 助手、404。
- AI 回答使用 `marked + DOMPurify` 渲染 Markdown。
- 商品图片支持本地上传，也保留 URL 添加能力；加载失败自动回退分类占位图。
- AI 助手支持会话列表、历史消息恢复、删除会话、当前商品上下文和多轮对话。
- 路由懒加载，Element Plus 按需注册，Vue/Element/图标/Markdown/HTTP 已独立分包。

### Docker Compose（Phase 12 已提交配置）

- `docker-compose.yml`：MySQL 8.0、Redis 7、Backend、Frontend。
- `backend.Dockerfile`：Maven 多阶段构建，运行镜像为 Temurin 17 JRE，并安装 `libgomp1` 供 DJL/ONNX 使用。
- `frontend/Dockerfile`：Node 22 构建静态资源，Nginx 托管并反向代理 `/api` 到后端。
- `frontend/nginx.conf`：SPA fallback 与 `/api` 代理。
- `src/main/resources/application-docker.yml`：通过 `SPRING_PROFILES_ACTIVE=docker` 读取环境变量，不包含明文密钥。
- `.dockerignore` 排除本机 `application.yml`，避免 Docker 镜像打包进 MySQL 密码和 DeepSeek Key。
- `.env.example`：Compose 环境变量模板。

### 后端工程化（Phase 13 已实现）

- Validation：所有请求 DTO、路径 ID、分页和筛选参数已接入 Jakarta Validation。
- 统一异常：覆盖业务异常、请求体校验、方法参数校验、JSON 解析、参数类型、缺失参数、访问拒绝和未知异常。
- 日志切面：Controller/Service 记录类名、方法名、参数摘要、耗时和异常；密码、JWT、API Key 与完整消息正文不落日志。
- Swagger/OpenAPI：接入 SpringDoc 2.8.9，公开 Swagger UI 和 OpenAPI JSON，配置 Bearer JWT 认证入口。
- 文档：README 已扩展，新增 `docs/er-diagram.md` 和 `docs/architecture.md` Mermaid 图。

### 最终优化（Phase 14 已完成）

- 图片上传：新增 `POST /api/files/images`、公开读取、文件头校验、UUID 文件名和 Compose 持久化卷。
- 商品状态：新增 `PATCH /api/items/{id}/relist`，卖家可重新上架 `OFF_SHELF` 商品，不能上架已售商品。
- 卖家详情：新增 `GET /api/items/{id}/manage`，公开详情仍只返回在售商品。
- AI 持久化：新增 `ai_conversation`、`ai_message`，提供会话列表、创建、消息查询、发送和删除接口。
- 多轮记忆：每次只加载最近 10 条消息，防止上下文无限增长。
- Redis 搜索缓存：使用版本号缓存键和 90 秒 TTL，商品写操作和订单取消时提升版本号使旧缓存失效。
- 订单状态机：`CREATED -> CONFIRMED -> IN_PROGRESS -> COMPLETED`，中途允许买卖双方取消，完成/取消为终态。
- 后端安全：JWT 黑名单键改为 token SHA-256 摘要；卖家管理详情认证规则已提前，避免被公开商品规则覆盖。
- 前端生产构建：主包由约 1.24MB 拆分到按路由加载，Element Plus 从全量安装改为按需注册。

## 3. 已验证结果

- 后端 `mvn compile` 通过。
- 前端 `npm run build` 通过。
- Vue 页面桌面/移动视口验证通过，无控制台错误。
- AI Agent 真实返回商品详情并正确渲染 Markdown。
- `docker compose config --quiet` 通过。
- 本机已成功构建镜像：
  - `campusai-market-backend:latest`
  - `campusai-market-frontend:latest`
- `docker compose up -d` 完整栈已启动：MySQL/Redis Healthy，Backend `Started CampusaiMarketApplication`，Frontend Nginx 正常。
- 验证通过：`http://localhost:8081/` 返回前端页面，`GET /api/categories` 返回中文分类，`POST /api/auth/login`（bob/123456）返回 token。
- RAG Embedding 已改为优先使用本地 `.model-cache` 挂载（`file:/models/...`），避免容器从 hf-mirror 下载到空文件导致重启。
- `sql/init.sql` 已补 `SET NAMES utf8mb4`，并加入 `bob`、`buyer1` 演示账号；Docker MySQL 初始化后的中文分类已验证无乱码。
- 已新增根目录 `README.md`，包含 Docker Compose 启动说明、端口、环境变量与 Embedding 模型说明。
- Phase 13：`mvn clean test` 通过，共 5 个测试；其中 `GlobalExceptionHandlerWebTest` 覆盖 4 类非法参数。
- Phase 13：本地真实启动后，`/swagger-ui/index.html` 返回 200，`/v3/api-docs` 返回 OpenAPI 3.1.0 且包含 Bearer scheme。
- Phase 13：空用户名、短密码、非法 JSON、负价格均返回统一 `code=400` 和中文错误。
- Phase 13：日志实测不记录密码或 JWT 原文，仅记录 DTO 类型和字符串长度。
- Phase 14：`mvn clean test` 共 15 个测试全部通过，覆盖校验、上传安全、订单状态机、RAG 降级和 Tool 调用预算。
- Phase 14：`npm run build` 通过，主入口约 11.5KB，最大异步页面约 8.8KB，无单块超过 500KB 警告。
- Phase 14：Docker Compose 四服务重建成功，前端 8081 返回 200，增量迁移已应用到 Compose MySQL。

## 4. Git 状态

- 分支：`main`
- Phase 13 实现提交：`401f09f feat: complete Phase 13 engineering hardening`
- 工作区：Phase 13 已完成本地提交；不会自动创建或推送 GitHub 远程仓库。
- 无 Git 远程仓库，尚未 push 到 GitHub。
- 敏感文件应继续忽略：
  - `src/main/resources/application.yml`
  - `.env`
  - `.model-cache/`
  - `frontend/node_modules/`
  - `frontend/dist/`

## 5. 剩余工作

### 5.1 Phase 12：已完成

- `.env` 已从本机 `application.yml` 生成（Git 忽略），Compose 配置校验通过。
- `docker compose up -d` 完整栈已启动并验证：MySQL、Redis、Backend、Frontend 均正常。
- Docker MySQL 中文乱码已修复：`sql/init.sql` 增加 `SET NAMES utf8mb4`，并重建测试卷验证。
- Embedding 模型网络下载问题已处理：默认仍走 hf-mirror，也可通过 `.env` 指向 `.model-cache` 本地挂载。
- 启动说明已写入根目录 `README.md`。
- 演示账号：`bob / 123456`、`buyer1 / 123456`。

### 5.2 Phase 13：后端健壮性与工程优化

建议按下面顺序执行，每完成一项都运行对应验证后再继续：

1. [x] 参数校验
   - 在 `pom.xml` 引入 `spring-boot-starter-validation`。
   - 检查 `src/main/java/com/campusmarket/dto/` 下全部请求 DTO。
   - 为注册、登录、商品发布/修改、订单创建、状态修改等补充 `@NotBlank`、`@Size`、`@NotNull`、`@DecimalMin`、`@Positive` 等约束。
   - Controller 参数增加 `@Valid`。
2. [x] 统一异常处理
   - 在 `GlobalExceptionHandler` 增加 `MethodArgumentNotValidException`、`ConstraintViolationException`、`HttpMessageNotReadableException` 的统一返回。
   - 返回结构继续使用 `ApiResponse`，不要把异常栈暴露给客户端。
   - 至少验证：空用户名、短密码、负价格、非法 JSON 都返回明确中文错误。
3. [x] 日志切面
   - 新建 `aspect` 包，增加 Controller/Service 调用日志。
   - 记录请求方法、类名/方法名、参数摘要、耗时、成功/异常；不要记录密码、JWT、API Key。
   - 保持日志简洁，避免循环或把大对象完整打印。
4. [x] Swagger/OpenAPI
   - 引入 SpringDoc OpenAPI 依赖。
   - 配置 JWT Bearer 认证入口。
   - 为 Controller 与主要 DTO/VO 补充必要注解，保证 `/swagger-ui/index.html` 可用。
5. [x] README 扩展
   - 补充项目背景、功能清单、技术栈、系统架构、数据库表说明、接口概览、演示账号、Docker 与本地运行方式。
   - 结合已有的 `README.md` 增量完善，不重复写临时命令。
6. [x] ER 图与系统架构图
   - 放在 `docs/` 下，使用 Mermaid 或 PNG/SVG。
   - ER 图覆盖 `user`、`category`、`item`、`favorite`、`orders`。
   - 架构图覆盖 Vue/Nginx、Spring Boot、MySQL、Redis、DeepSeek、本地 Embedding/RAG。
   - README 中链接对应图片。
7. [ ] GitHub
   - 确认敏感文件仍被忽略后，创建远程仓库。
   - 配置 `origin`，推送 `main` 全部历史。
   - 推送后核对远程页面和 README 渲染。

当前按用户要求不创建远程仓库、不配置 `origin`、不执行 `git push`。

Phase 13 验收标准：

- [x] `mvn compile` 和必要测试通过。
- [x] 非法参数返回统一、可读的 `code=400` 错误。
- [x] 日志能显示关键调用和耗时，且不含密钥/密码。
- [x] Swagger 页面可打开、接口定义完整、可携带 JWT 调接口。
- [x] README 与新会话可独立完成本地运行和 Docker 部署。
- [ ] Git 工作区干净、远程仓库可访问（远程推送按用户要求暂缓）。

### 5.3 已完成的前端与 AI 增强

- [x] 真实图片上传接口、文件校验与 Docker 持久化。
- [x] 已下架商品重新上架接口与按钮。
- [x] 卖家查看自己已售/已下架商品的详情视图。
- [x] Vite 路由懒加载与依赖分包，消除大 chunk 警告。
- [x] `ai_conversation` / `ai_message` 表与服务端多轮记忆。
- [x] Agent 搜索结果 Redis 版本化缓存与写操作失效。
- [x] `CREATED / CONFIRMED / IN_PROGRESS / COMPLETED / CANCELLED` 订单状态流转。

### 5.4 后续可选增强

- 中文 Embedding 模型：当前 `all-MiniLM-L6-v2` 可稳定运行；若替换，应先建立中文检索评测集，再评估 BGE 等 ONNX 模型。
- 对象存储：当前本地卷适合单实例展示；多实例部署时替换为 OSS/S3。
- Agent 工具调用轨迹：当前前端只展示能力，不展示内部 Tool 调用详情；如果需要面试演示，可增加脱敏的调用摘要。

## 6. 本地运行方式

### 本地开发（不使用 Docker）

- MySQL 本机服务：`MySQL80`，端口 3306。
- Redis：`docker start campusai-redis`（需要 Docker Desktop）。
- 本地配置：`src/main/resources/application.yml` 读取 `.env` 中的 `LOCAL_MYSQL_USERNAME` / `LOCAL_MYSQL_PASSWORD`、DeepSeek Key 和 JWT Secret。
- 后端启动：

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd' -f 'D:\CampusAI Market\pom.xml' spring-boot:run
```

- 前端启动：

```powershell
cd 'D:\CampusAI Market\frontend'
npm install
npm run dev
```

- 本地前端默认 `http://127.0.0.1:5173/`，Vite 将 `/api` 代理到 8080。

### Docker Compose 运行

- 前端：`http://localhost:8081/`
- 后端 API：`http://localhost:8080/`
- MySQL 与 Redis 不映射宿主机端口，避免与本机 MySQL/Redis 冲突。

## 7. 已知问题与注意事项

- Docker 访问 Docker Hub 不稳定时，可先单独 `docker pull` 基础镜像再执行 Compose 构建。
- RAG 在首次规则类提问时加载模型；下载失败只会跳过规则增强，不影响后端启动。
- 若容器使用本地模型，按 README 在 `.model-cache/` 放入 `model.onnx` 与 `tokenizer.json`，并在 `.env` 中设置 `EMBEDDING_MODEL_URI=file:/models/model.onnx`、`EMBEDDING_TOKENIZER_URI=file:/models/tokenizer.json`。
- Compose 的 MySQL 数据来自 `sql/init.sql`，本机 MySQL80 里的测试数据不会自动带入；删除 Compose 卷前请确认不需要其中数据。
- `application.yml` 只保存环境变量占位，真实密钥放在 Git 忽略的 `.env` 中。
- 目录名含空格，所有 PowerShell、Maven、Git、Docker 路径都要加引号。
- `README.md`、Swagger、参数校验 starter、统一异常和日志切面均已完成。
- AI 对话已使用服务端会话与消息表持久化，旧 `/api/agent/chat` 仅保留兼容。
- 商品图片支持本地上传和 URL，Compose 使用 `uploads_data` 卷持久化。

## 8. 新会话建议开场提示词

```text
请先阅读项目根目录 D:\CampusAI Market\NEXT_STEPS.md 与 CONTEXT.md，
先不要修改代码，用中文说明你对当前项目状态和剩余工作的理解。
当前最新提交以 `git log -1` 为准，Phase 14 本地实现与验证已完成但尚未提交。
除 GitHub 远程创建与推送外，图片、AI 会话、订单状态机、参数校验、Swagger、README 和图表均已完成。
项目目录含空格，命令中请始终使用引号。
```

## 9. 建议下一步

Phase 14 本地实现与验证完成。后续如需发布，只需创建 GitHub 远程仓库、配置 `origin` 并推送 `main`。
