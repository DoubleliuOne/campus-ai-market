# CampusAI Market 项目上下文

> 本文档用于让下一个 Codex 会话在只读取本项目文件时快速恢复上下文。
> 最后更新：2026-09-06

## 1. 项目一句话定位

CampusAI Market 是一个面向校园二手交易场景的 Java 后端 + Spring AI Agent 项目：

- 普通业务：用户注册登录、商品发布/搜索/详情/修改/下架、收藏、订单。
- AI 能力：通过 `/api/agent/chat` 让 DeepSeek 基于 Spring AI Tool Calling 调用真实 Java 后端能力查询 MySQL，并基于 RAG 回答校园交易规则问题。
- 目标用户：准备 Java 后端实习面试的学生；项目重点是“AI 真正参与业务”，不是简单聊天框。

## 2. 项目根目录与环境

- 项目根目录：`D:\CampusAI Market`
- 注意：目录名包含空格；PowerShell、Maven、Git 命令都要用引号包住路径。
- 项目内已有独立 Git 仓库（分支 `main`）。
- `D:\` 根目录本身还有一个用户较早误建的大仓库；不要在 `D:\` 根仓库里提交本项目，必须使用 `D:\CampusAI Market\.git`。
- IDEA：IntelliJ IDEA 2025.2.3，使用其自带 Maven 3，IDEA Maven 路径：
  `C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\plugins\maven\lib\maven3`
- IDEA 中运行 Spring Boot 使用的 JDK 是 OpenJDK 25，路径 `C:\Users\LENOVO\.jdks\openjdk-25`。
- Maven 命令行（`mvn.cmd`）不在 PATH；本机也存在 `D:\java`（JDK 23），用 Maven 命令行启动项目时会以 Java 23 运行。
- 项目语言级别固定为 Java 17（`pom.xml` 中 `<java.version>17</java.version>`）。
- MySQL：本机 Windows 服务 `MySQL80`，默认 3306。
- Redis：Docker 容器，名称为 `campusai-redis`，端口 6379，镜像 `redis:7`。启动命令：
  `docker start campusai-redis`
- Docker Desktop 需要先运行；若 Redis 未启动，代码已做降级处理，但 JWT 黑名单不可用。
- Docker Compose 完整栈使用独立 MySQL/Redis 容器与 `campusai-market_mysql_data` 卷，和本机 MySQL80、`campusai-redis` 互不共享数据。
- 本机 Embedding 模型缓存目录 `.model-cache/`（Git 忽略）可被 Compose 只读挂载到容器 `/models`，避免运行时下载失败。

## 3. 关键技术版本

- Spring Boot：`3.5.4`
- Spring AI：`1.1.8`（通过 `spring-ai-bom` 管理）
- Java 目标：17
- MyBatis-Plus：`mybatis-plus-spring-boot3-starter:3.5.9`
- MyBatis-Plus 分页额外依赖：`mybatis-plus-jsqlparser:3.5.9`
- MySQL 驱动：由 Spring Boot 管理，实际解析为 `mysql-connector-j 9.3.0`
- JWT：`com.auth0:java-jwt:4.4.0`
- Spring Security：由 Spring Boot 管理，实际解析为 `spring-security 6.5.2`
- Redis：Spring Data Redis（Lettuce）
- DeepSeek：通过 `spring-ai-starter-model-openai` 以 OpenAI 兼容方式接入
- 本地 Embedding：`spring-ai-starter-model-transformers` + `spring-ai-vector-store`

## 4. 运行方式

### 4.1 必须存在但已被 Git 忽略的本地文件

`src/main/resources/application.yml` 已在 `.gitignore` 中忽略，因为它包含：

- MySQL root 密码
- DeepSeek API Key

该文件只存在于本机 `D:\CampusAI Market\src\main\resources\application.yml`。下一个会话应先读取它，但不要把真实密码/API Key 写进任何提交文件。

`application.yml` 当前结构：

```yaml
server.port: 8080

spring.datasource:
  url: jdbc:mysql://localhost:3306/campusai_market?...&useSSL=false
  username: root
  password: <本机真实 MySQL 密码>

spring.data.redis:
  host: localhost
  port: 6379

spring.ai.openai:
  base-url: https://api.deepseek.com
  api-key: <DeepSeek Key>
  chat.options.model: deepseek-chat

spring.ai.embedding.transformer:
  cache.enabled: true
  onnx.model-uri: https://hf-mirror.com/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx
  tokenizer.uri: https://hf-mirror.com/sentence-transformers/all-MiniLM-L6-v2/resolve/main/tokenizer.json

jwt:
  secret: <本机配置，长度足够 HMAC256>
  expiration-minutes: 1440
```

### 4.2 启动

- 在 IDEA 中运行 `com.campusmarket.CampusaiMarketApplication`。
- 或命令行：

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd' -f 'D:\CampusAI Market\pom.xml' spring-boot:run
```

- 成功日志标志：

```text
Tomcat started on port 8080
Started CampusaiMarketApplication
```

- 需要隔离测试另一个实例时使用：

```powershell
... spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

## 5. 已完成功能

### 5.1 用户认证

- 注册：`POST /api/auth/register`
- 登录：`POST /api/auth/login`，返回 JWT
- 当前用户：`GET /api/auth/me`
- 登出：`POST /api/auth/logout`，把 JWT 加入 Redis 黑名单
- 密码：BCrypt 哈希后存 `user.password_hash`
- 鉴权：无状态 JWT + Spring Security Filter

### 5.2 商品模块

- 发布：`POST /api/items`，需要登录
- 列表搜索：`GET /api/items?keyword=&categoryId=&minPrice=&maxPrice=&page=&size=`，公开
- 热门列表：`GET /api/items/hot`，公开
- 卖家自己的商品：`GET /api/items/mine?status=`，需要登录
- 详情：`GET /api/items/{id}`，公开，只返回 `ON_SALE`
- 修改：`PUT /api/items/{id}`，仅卖家本人、仅 `ON_SALE`
- 下架：`DELETE /api/items/{id}`，逻辑下架，改为 `OFF_SHELF`

### 5.3 收藏模块

- 添加收藏：`POST /api/favorites`
- 取消收藏：`DELETE /api/favorites/{itemId}`
- 我的收藏：`GET /api/favorites`

全部需要 JWT；数据库唯一约束防止重复收藏。

### 5.4 订单模块

- 创建订单：`POST /api/orders`，事务内把商品 `ON_SALE -> SOLD`
- 我的订单：`GET /api/orders/my?role=buyer|seller`
- 订单状态：`PATCH /api/orders/{id}/status`
  - 买家可取消 `CREATED -> CANCELLED`，商品恢复 `ON_SALE`
  - 卖家可完成 `CREATED -> COMPLETED`

创建订单使用条件更新：

```text
UPDATE item SET status = 'SOLD' WHERE id = ? AND status = 'ON_SALE'
```

用于防止并发超卖。

### 5.5 Redis

- JWT 登出黑名单：
  - Key：`auth:blacklist:<token>`
  - Value：`1`
  - TTL：token 剩余有效期
- 热门商品缓存：
  - Key：`cache:item:hot`
  - TTL：300 秒
  - 商品发布、修改、下架、被下单、订单取消时主动删除缓存
- Redis 故障降级：JWT 过滤器不再因 Redis 不可用把所有请求判为 401，只验证 JWT 签名；黑名单功能暂时失效。

### 5.6 AI / Agent / RAG

- 基础聊天接口：
  - `POST /api/agent/chat`
  - 请求：`{"message":"...", "itemId": 可选}`
  - 需要 JWT
- DeepSeek 模型：`deepseek-chat`
- 四个 Tool：

| Tool | 作用 |
|---|---|
| `searchItems` | 按关键词/分类/价格分页搜索在售商品 |
| `getItemDetail` | 按商品 id 查详情 |
| `getMyOrders` | 查当前 JWT 用户自己的订单 |
| `recommendItems` | 按用户需求和预算搜索后推荐 |

- Tool 注册方式：
  - `ChatClient.Builder.defaultTools(itemTools, orderTools)`
  - `@Tool` 注解方法
- 用户身份传递：
  - Controller 从 `@AuthenticationPrincipal` 得到 `LoginUser`
  - 放入 `ChatClient.prompt().toolContext(...)`
  - Tool 方法参数 `ToolContext` 读取 `userId`
- 商品详情问答：
  - 请求可带 `itemId`
  - 后端先取真实商品详情并注入用户消息
  - System Prompt 要求 AI 参数缺失时说明缺失，不得编造
- RAG：
  - 知识文档：`src/main/resources/knowledge/trading-rules.txt`
  - 文档切分为 5 个规则段落
  - 本地 ONNX Embedding 模型生成向量
  - `SimpleVectorStore` 内存向量库
  - 命中规则关键词时用 `similaritySearch` 取 Top 3 注入上下文

### 5.7 Vue 3 前端（Phase 11 主体已完成，尚未提交）

- 目录：`frontend/`，Vue 3 + Vite 6 + Vue Router + Element Plus + Axios + lucide 图标。
- 本地开发：`npm install` 后 `npm run dev`，默认 `http://127.0.0.1:5173/`，Vite 把 `/api` 代理到 `http://localhost:8080`。
- 页面：登录/注册、首页商品市场、商品详情、发布/编辑商品、我的商品、我的收藏、我的订单、AI 助手、404。
- 会话：JWT 存 `localStorage`，Axios 请求自动带 `Authorization`，401 自动清会话并回登录页。
- 商品图片后端只保存 URL；前端支持最多 5 个图片 URL 输入与预览，加载失败时按分类显示占位图。
- AI 助手回答使用 `marked + DOMPurify` 做安全 Markdown 渲染；带 `itemId` 时每轮请求都会携带当前商品上下文。
- 为 Phase 11 新增的两个后端契约：
  - `GET /api/categories`：公开分类列表，供搜索筛选和发布页使用。
  - `GET /api/favorites/check/{itemId}`：登录用户查询是否已收藏某商品。
- 页面已通过桌面与移动视口检查；商品搜索、登录、收藏切换、AI 真实问答均在前端页面验证通过。

### 5.8 Docker Compose（Phase 12 完整栈已启动验证）

- 四服务：MySQL 8.0、Redis 7、Backend、Frontend，`docker compose up -d` 可一键启动。
- MySQL 通过 `sql/init.sql` 自动初始化建表、默认分类与演示账号 `bob`、`buyer1`；脚本开头已加 `SET NAMES utf8mb4`，中文初始化无乱码。
- Embedding 模型默认使用 hf-mirror URL；本机通过 `.env` 中 `EMBEDDING_MODEL_URI` / `EMBEDDING_TOKENIZER_URI` 指向 `.model-cache` 本地文件，避免容器下载空文件。
- 前端 `http://localhost:8081/`，后端 API `http://localhost:8080/`，Nginx 已反代 `/api`。
- 已新增根目录 `README.md` 记录运行方式、端口、环境变量与模型说明。

## 6. 数据库

建表脚本：`sql/init.sql`

数据库名：`campusai_market`

表：

- `user`
- `category`
- `item`
- `favorite`
- `orders`

关键设计：

- 字符集 `utf8mb4`
- 金额 `DECIMAL(10,2)`
- 商品状态字符串：`ON_SALE / SOLD / OFF_SHELF`
- 订单状态字符串：`CREATED / PAID / COMPLETED / CANCELLED`
- `favorite` 唯一键 `(user_id, item_id)`
- 表名 `orders`，避免 `order` 关键字
- 分类默认数据：数码产品、教材、宿舍用品、生活用品、其他

## 7. 代码分层与文件结构

包根：`com.campusmarket`

```text
ai/
  rag/KnowledgeBaseService.java
  rag/RagConfig.java
  service/AiChatService.java
  tool/ItemTools.java
  tool/OrderTools.java
common/
  ApiResponse.java
  PageResult.java
config/
  AppConfig.java
  MybatisPlusConfig.java
controller/
  AgentController.java
  AuthController.java
  FavoriteController.java
  ItemController.java
  OrderController.java
dto/
entity/
exception/
  BusinessException.java
  GlobalExceptionHandler.java
mapper/
security/
  SecurityConfig.java
  JwtAuthenticationFilter.java
  JwtUtil.java
  LoginUser.java
service/
vo/
```

Controller 不写业务；Service 负责业务；Mapper 使用 MyBatis-Plus `BaseMapper`；统一返回 `ApiResponse`；业务错误由 `GlobalExceptionHandler` 转成 `code=400`。

前端 `frontend/src/` 也按功能分层：

```text
api/      Axios 实例与后端接口封装
router/   路由与登录守卫
stores/   登录会话状态
layouts/  顶部导航与整体布局
components/ 商品卡片、图片占位、状态标签、空状态
views/    各页面视图
utils/    金额与时间格式化
styles/   全局 CSS 变量与通用样式
```

## 8. 已提交 Git 历史

当前 `main` 分支最新提交：`6bbc90b feat: add Docker Compose deployment for Phase 12`

提交顺序：

```text
47ea56d chore: init Spring Boot 3.5.4 project
e77041f feat: add user register, JWT login and Spring Security
4cc1d8a feat: add item module
9af8fb4 feat: add favorite module
5cd2eda feat: add order module and seller item list
b813e22 feat: add Redis JWT blacklist and hot item cache
297c412 feat: add DeepSeek chat and searchItems tool
7d5a483 feat: add getItemDetail tool and Redis failure fallback
da1f759 feat: add getMyOrders tool with user context
385f140 feat: add recommendItems tool and complete Tool Calling phase
b77e95e feat: optimize agent with item context and honest answers
493865c feat: add RAG trading rule knowledge base
0c2cb13 docs: add project context
5043010 feat: add Vue 3 frontend and category/favorite APIs
6bbc90b feat: add Docker Compose deployment for Phase 12
```

截至当前文档，Phase 12 收尾修复（README、init.sql、Compose 模型配置、忽略规则等）仍未提交；提交时不要把 `application.yml`、`.env`、`.model-cache/` 带入。

## 9. 已验证成功的内容

以下均为实际运行/接口验证通过：

- Spring Boot 3.5.4 启动，端口 8080
- MySQL 建库建表和默认分类插入
- MyBatis-Plus 通过 `UserMapper.selectCount` 真实查询 MySQL
- 注册、登录、`/me`、登出黑名单（登出后同 token 返回 401）
- 商品发布、列表、关键词/分类/价格分页查询、详情、修改、下架、卖家商品列表
- 收藏、取消收藏、我的收藏
- 订单创建、买家视角查询、卖家视角查询、状态修改、防重复购买
- Redis 登出黑名单 key 与热门商品缓存 key 可查询到
- DeepSeek 基础聊天返回中文
- `searchItems`：问“200元以内机械键盘”，AI 真实调用工具并返回“机械键盘 白色 99元”
- `getItemDetail`：AI 查询 id=5 并返回真实详情
- `getMyOrders`：buyer1 只能看到自己买到的订单；bob 只能看到自己卖出的订单
- `recommendItems`：推荐 300 元以内机械键盘，基于真实商品回答
- Phase 9：带 `itemId` 的商品问答不编造缺失参数；搜索不到显卡时 AI 诚实告知无结果
- Phase 10 RAG：能正确回答“不能发布什么”“退款规则”“交易注意事项”
- 多个功能改动后均通过 Maven `compile`（退出码 0）
- Vue 前端开发服务器、登录、首页真实商品、收藏切换、页面导航均通过浏览器验证
- AI 助手返回内容可正确渲染 Markdown，不再显示 `**`、`##` 等原文标记
- Phase 12：`docker compose up -d` 完整栈启动成功，MySQL/Redis Healthy，后端 `Started`，前端 8081 返回页面。
- Phase 12：分类接口返回正确中文；`bob/123456` 登录返回 token；`/api/items/hot` 与分页搜索接口均返回 200。

## 10. 当前已知问题与注意点

1. `application.yml` 被 Git 忽略，克隆到新机器后必须重新创建并填入 MySQL 密码与 DeepSeek Key。
2. DeepSeek 官方接口当前不提供 Embedding，因此 RAG 向量化使用本地 `all-MiniLM-L6-v2`。
3. 本地模型首次启动会通过 `hf-mirror.com` 下载模型，并下载 DJL 的 PyTorch 原生库；首次启动较慢（实测约 30 秒到几分钟），之后模型有本地缓存。
4. 依赖包内自带的 `onnx/all-MiniLM-L6-v2/model.onnx` 是 133 字节占位文件，不能直接使用；必须使用配置中的 hf-mirror 地址，不能回退到 `classpath:onnx/.../model.onnx`。
5. `all-MiniLM-L6-v2` 不是中文专用模型，中文语义检索只是“可用级别”；若后续要提升，可换 BGE 等中文模型。
6. `SimpleVectorStore` 是内存向量库，重启后需要重新加载文档和生成向量。
7. Agent 对话目前没有保存历史，每次 `/api/agent/chat` 是独立请求；尚未实现 conversation/message 持久化。
8. `/api/agent/chat` 返回 DeepSeek 错误时可能经过 `/error` 被安全过滤器包装成 `401 未登录`，排查时看应用日志比看 HTTP body 更准确。
9. Spring AI 的 OpenAI 兼容 base-url 必须是 `https://api.deepseek.com`，不能写成 `/v1`，否则 DeepSeek 返回 404。
10. Windows PowerShell 5.1 用字符串发送中文 JSON 会乱码；测试时先把 JSON 转成 UTF-8 字节再作为 `-Body`。PowerShell 控制台显示乱码不代表 API 返回乱码，可用 `curl` 或由后端日志确认。
11. 项目目录名包含空格；后续 Docker Compose、shell 脚本要全程加引号。
12. 当前没有参数校验 starter、没有 Swagger/OpenAPI、没有统一 JSON 解析错误处理、没有日志切面；这些属于后续优化阶段。
13. 前端暂没有图片上传接口，发布商品只能填图片 URL；如需真实文件上传需要后端加存储/静态资源能力。
14. AI 助手对话只保存在当前前端页面内存，刷新后即清空；服务端 conversation/message 持久化仍未做。
15. 前端生产构建有单 chunk 超过 500KB 的提示，属于体积优化项，可放到 Phase 13 做路由懒加载与手动分包。
16. Docker/Nginx 已配置 `/api` 反代；Vite 代理只服务于本地开发。
17. Compose MySQL 由 `sql/init.sql` 初始化，本机 MySQL80 里的数据不会自动进入 Compose；若 Docker 内 hf-mirror 下载 Embedding 模型为空文件，使用 `.model-cache` 本地挂载。

## 11. 下一步计划

按原项目阶段：

- Phase 11：Vue 3 + Element Plus 前端主体已完成；可选补充：真实图片上传、编辑商品页体验优化、商品详情对已售/下架商品的卖家视图
- Phase 12：Docker Compose（MySQL、Redis、Backend、Frontend）完整栈已启动验证
- Phase 13：项目优化与文档（参数校验、统一异常、日志切面、Swagger/OpenAPI、README 扩展、ER 图、架构图、GitHub 仓库）

后续可选增强：

- AI 对话记录表 `conversation` / `message`
- 多轮 Agent 记忆
- 中文 Embedding 模型
- AI 搜索结果 Redis 缓存
- 更完整的订单状态流转
- MCP（按用户要求暂缓）

## 12. 测试提示

本地开发账号（仅为测试数据，随时可删除）：

- `bob / 123456`（id 1，seller/普通用户）
- `buyer1 / 123456`（id 2，买家）

常用请求示例：

```powershell
# 登录
$loginBody = @{ username = "bob"; password = "123456" } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/login" -ContentType "application/json" -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.data.token)" }

# 带中文请求体时使用 UTF-8 字节
$obj = @{ message = "帮我找200元以内的机械键盘" }
$json = $obj | ConvertTo-Json
$bytes = [System.Text.Encoding]::UTF8.GetBytes(($json | Out-String).Trim())
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/agent/chat" -Headers $headers -ContentType "application/json" -Body $bytes
```
