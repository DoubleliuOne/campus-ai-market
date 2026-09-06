# CampusAI Market 新会话交接说明

> 新 Codex 会话请先阅读本文件与 `CONTEXT.md`，再开始修改。
> 创建时间：2026-09-06
> 最后更新：2026-09-06（Phase 12 完整栈已验证）

## 1. 项目一句话现状

CampusAI Market 是一个面向校园二手交易与 Java 后端面试展示的项目。当前已完成后端业务、AI Tool Calling、RAG、Vue 3 前端和 Docker Compose 完整栈启动验证；还剩项目优化、文档完善与 GitHub 推送。

## 2. 当前完成范围

### 后端（已完成）

- 用户：注册、登录、JWT、`/me`、登出 Redis 黑名单。
- 商品：发布、搜索、热门缓存、详情、修改、下架、卖家商品列表。
- 收藏：添加、取消、我的收藏、是否已收藏检查。
- 订单：创建订单防并发超卖、买家/卖家订单查询、取消/完成。
- AI Agent：DeepSeek Tool Calling，工具包括 `searchItems`、`getItemDetail`、`getMyOrders`、`recommendItems`。
- RAG：本地 `all-MiniLM-L6-v2` + `SimpleVectorStore`，回答平台交易规则。
- 为前端补充的公开接口：`GET /api/categories`、`GET /api/favorites/check/{itemId}`。

### 前端（Phase 11 已完成）

- Vue 3 + Vite 6 + Vue Router + Element Plus + Axios + lucide。
- 页面：登录/注册、首页搜索、商品详情、发布/编辑、我的商品、我的收藏、我的订单、AI 助手、404。
- AI 回答使用 `marked + DOMPurify` 渲染 Markdown。
- 商品图片为 URL 输入，无文件上传接口。

### Docker Compose（Phase 12 已提交配置）

- `docker-compose.yml`：MySQL 8.0、Redis 7、Backend、Frontend。
- `backend.Dockerfile`：Maven 多阶段构建，运行镜像为 Temurin 17 JRE，并安装 `libgomp1` 供 DJL/ONNX 使用。
- `frontend/Dockerfile`：Node 22 构建静态资源，Nginx 托管并反向代理 `/api` 到后端。
- `frontend/nginx.conf`：SPA fallback 与 `/api` 代理。
- `src/main/resources/application-docker.yml`：通过 `SPRING_PROFILES_ACTIVE=docker` 读取环境变量，不包含明文密钥。
- `.dockerignore` 排除本机 `application.yml`，避免 Docker 镜像打包进 MySQL 密码和 DeepSeek Key。
- `.env.example`：Compose 环境变量模板。

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

## 4. Git 状态

- 分支：`main`
- 最新提交：`6bbc90b feat: add Docker Compose deployment for Phase 12`
- 工作区：Phase 12 收尾修复尚未提交（`README.md`、`sql/init.sql`、`application-docker.yml`、`docker-compose.yml`、忽略规则与文档更新）。
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

- 引入 `spring-boot-starter-validation`，为 DTO 增加 `@NotBlank`、`@Size`、`@DecimalMin` 等参数校验。
- 补充参数校验异常与 JSON 解析异常的统一处理。
- 增加日志切面，记录 Controller/Service 调用、耗时、异常。
- 接入 Swagger/OpenAPI，形成接口文档。
- 编写项目 README。
- 绘制 ER 图与系统架构图。
- 创建 GitHub 仓库并推送全部历史。

### 5.3 前端收尾项（可选）

- 真实图片上传接口与静态资源存储。
- 已下架商品重新上架接口与按钮。
- 卖家查看自己已售/已下架商品的详情视图。
- 生产构建分包，消除单 chunk 超 500KB 警告。

### 5.4 AI 可选增强

- `conversation` / `message` 表与服务端多轮记忆。
- 中文 Embedding 模型替换 `all-MiniLM-L6-v2`。
- Agent 搜索结果 Redis 缓存。
- 更完整订单状态流转。

## 6. 本地运行方式

### 本地开发（不使用 Docker）

- MySQL 本机服务：`MySQL80`，端口 3306。
- Redis：`docker start campusai-redis`（需要 Docker Desktop）。
- 本地配置：`src/main/resources/application.yml`，含本机 MySQL 密码、DeepSeek Key、JWT Secret。
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
- Compose 首次启动会访问 `hf-mirror.com` 下载 Embedding 模型。
- 若容器下载 hf-mirror 文件得到 0 字节导致后端重启，按 README 在 `.model-cache/` 放入 `model.onnx` 与 `tokenizer.json`，并在 `.env` 中设置 `EMBEDDING_MODEL_URI=file:/models/model.onnx`、`EMBEDDING_TOKENIZER_URI=file:/models/tokenizer.json`。
- Compose 的 MySQL 数据来自 `sql/init.sql`，本机 MySQL80 里的测试数据不会自动带入；删除 Compose 卷前请确认不需要其中数据。
- `application.yml` 含真实密钥，任何提交前都要确认被 `.dockerignore` / `.gitignore` 排除。
- 目录名含空格，所有 PowerShell、Maven、Git、Docker 路径都要加引号。
- 当前没有 `README.md`、Swagger、参数校验 starter、日志切面。
- AI 对话目前是前端内存会话，服务端无历史持久化。
- 商品图片没有文件上传能力，只有 URL 输入。

## 8. 新会话建议开场提示词

```text
请先阅读项目根目录 D:\CampusAI Market\NEXT_STEPS.md 与 CONTEXT.md，
先不要修改代码，用中文说明你对当前项目状态和剩余工作的理解。
然后按 NEXT_STEPS.md 从下一个未完成阶段开始执行，每完成一个阶段更新文档。
项目目录含空格，命令中请始终使用引号。
```

## 9. 建议下一步

Phase 12 完整栈已启动验证。下一步进入 Phase 13：参数校验、统一异常/日志、Swagger、README 扩展、ER/架构图，最后创建 GitHub 仓库并推送。
