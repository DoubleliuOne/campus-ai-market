# CampusAI Market

CampusAI Market 是一个面向校园二手交易场景的 Spring Boot + Spring AI 项目，同时作为 Java 后端面试展示作品。项目不只提供传统商品交易接口，还把 DeepSeek Tool Calling 与本地 RAG 接入真实业务数据。

## 项目背景

校园二手交易通常需要覆盖商品发布、搜索、收藏、下单和订单状态流转，同时用户经常需要快速确认“有没有某类商品”“这件商品是否适合我”“平台交易规则是什么”。本项目用传统后端保证交易数据与状态一致性，再用 AI Agent 查询真实业务数据并回答规则问题，避免把 AI 做成脱离业务的聊天框。

## 核心功能

- 用户：注册、登录、JWT 鉴权、当前用户查询、Redis 登出黑名单。
- 商品：发布、分页搜索、分类与价格筛选、热门列表、详情、修改、下架、卖家商品列表。
- 收藏：添加、取消、是否已收藏、我的收藏。
- 订单：创建订单、并发防超卖、买家/卖家订单查询、取消与完成。
- AI Agent：DeepSeek Tool Calling 调用 `searchItems`、`getItemDetail`、`getMyOrders`、`recommendItems`。
- RAG：本地 `all-MiniLM-L6-v2` Embedding + `SimpleVectorStore`，回答校园交易规则。
- 前端：Vue 3 单页应用，覆盖登录注册、市场、详情、发布编辑、收藏、订单和 AI 助手。
- 工程化：Bean Validation、统一异常、Controller/Service 调用日志、Swagger/OpenAPI、Docker Compose。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.5.4、Spring MVC、Spring Security |
| AI | Spring AI 1.1.8、DeepSeek OpenAI 兼容接口、Transformers Embedding、SimpleVectorStore |
| 数据 | MyBatis-Plus 3.5.9、MySQL 8.0、Redis 7、Lettuce |
| 鉴权 | JWT（java-jwt）、BCrypt、Redis token 黑名单 |
| 接口文档 | SpringDoc OpenAPI 2.8.9、Swagger UI |
| 前端 | Vue 3、Vite 6、Vue Router、Element Plus、Axios、lucide |
| 部署 | Docker Compose、Nginx、Maven 多阶段构建 |

## 系统架构与数据模型

- [系统架构图](docs/architecture.md)
- [ER 图](docs/er-diagram.md)

架构覆盖 Vue/Nginx、Spring Boot、MySQL、Redis、DeepSeek 以及本地 Embedding/RAG。ER 图覆盖 `user`、`category`、`item`、`favorite`、`orders` 五张核心表。

## 项目结构

```text
src/main/java/com/campusmarket/
  ai/           Agent Chat、RAG、Tool Calling
  aspect/       Controller/Service 调用日志
  common/       统一响应与分页对象
  config/       OpenAPI、MyBatis-Plus、通用 Bean
  controller/   REST API
  dto/          请求参数与校验规则
  entity/       数据表实体
  exception/    业务异常与全局异常处理
  mapper/       MyBatis-Plus Mapper
  security/     JWT 与 Spring Security
  service/      业务逻辑
  vo/           接口响应对象
frontend/       Vue 3 前端
sql/init.sql    建库、建表与演示数据
docs/           ER 图与系统架构图
docker-compose.yml
```

## 数据库表

| 表 | 作用 | 关键约束 |
| --- | --- | --- |
| `user` | 买家和卖家账号 | `username` 唯一 |
| `category` | 商品分类 | `name` 唯一 |
| `item` | 二手商品 | 商品状态 `ON_SALE/SOLD/OFF_SHELF` |
| `favorite` | 用户收藏关系 | `(user_id, item_id)` 唯一 |
| `orders` | 订单与价格快照 | 订单状态 `CREATED/PAID/COMPLETED/CANCELLED` |

完整字段和关系见 [ER 图](docs/er-diagram.md)。

## 接口概览

| 模块 | 接口 |
| --- | --- |
| 认证 | `POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me`、`POST /api/auth/logout` |
| 分类 | `GET /api/categories` |
| 商品 | `POST /api/items`、`GET /api/items`、`GET /api/items/hot`、`GET /api/items/{id}`、`PUT /api/items/{id}`、`DELETE /api/items/{id}`、`GET /api/items/mine` |
| 收藏 | `POST /api/favorites`、`DELETE /api/favorites/{itemId}`、`GET /api/favorites`、`GET /api/favorites/check/{itemId}` |
| 订单 | `POST /api/orders`、`GET /api/orders/my`、`PATCH /api/orders/{id}/status` |
| AI | `POST /api/agent/chat` |

后端启动后访问以下地址查看完整参数、响应结构和 JWT 调试入口：

- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

在 Swagger UI 点击 `Authorize`，填写登录接口返回的 JWT 即可调用受保护接口。

## Docker Compose 运行

1. 安装并启动 Docker Desktop。
2. 在项目根目录创建环境变量文件：

```powershell
Copy-Item -LiteralPath '.env.example' -Destination '.env'
```

3. 填写 `.env`：

- `DEEPSEEK_API_KEY`：DeepSeek API Key。
- `JWT_SECRET`：至少 32 个随机字符。
- `MYSQL_ROOT_PASSWORD`、`MYSQL_USER`、`MYSQL_PASSWORD`：可保留开发默认值。

4. 构建并启动：

```powershell
docker compose -f 'D:\CampusAI Market\docker-compose.yml' up -d --build
```

5. 检查服务：

```powershell
docker compose -f 'D:\CampusAI Market\docker-compose.yml' ps
Invoke-RestMethod -Uri 'http://localhost:8081/' -Method Get
Invoke-RestMethod -Uri 'http://localhost:8080/api/categories' -Method Get
```

访问地址：

- 前端：`http://localhost:8081/`
- 后端 API：`http://localhost:8080/`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`

## 本地开发

### 前置服务

- MySQL 8.0，数据库名 `campusai_market`，默认端口 `3306`。
- Redis 7，默认端口 `6379`。本地容器可用 `docker start campusai-redis` 启动。
- 创建 `src/main/resources/application.yml`，配置 MySQL、Redis、DeepSeek 和 JWT。该文件包含真实密钥并且已被 Git 忽略。

### 后端

项目目录包含空格，PowerShell 命令中的路径必须加引号：

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd' -f 'D:\CampusAI Market\pom.xml' spring-boot:run
```

### 前端

```powershell
Set-Location -LiteralPath 'D:\CampusAI Market\frontend'
npm install
npm run dev
```

本地前端地址为 `http://127.0.0.1:5173/`，Vite 会把 `/api` 代理到 `http://localhost:8080`。

## 演示账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 卖家/普通用户 | `bob` | `123456` |
| 买家 | `buyer1` | `123456` |

演示数据由 `sql/init.sql` 初始化，仅用于本地开发和 Docker 测试。

## 参数校验与错误响应

请求 DTO、路径 ID、分页和筛选参数均使用 Jakarta Validation 校验。常见错误统一返回：

```json
{
  "code": 400,
  "message": "password: 密码长度必须在6到72个字符之间",
  "data": null
}
```

`GlobalExceptionHandler` 统一处理业务异常、请求体校验、方法参数校验、JSON 解析、参数类型错误、缺失参数和未知异常。未知异常不会把堆栈暴露给客户端。

## 日志

`CallLoggingAspect` 记录 Controller 与 Service 的类名、方法名、参数摘要、成功/异常和耗时。字符串只记录长度，DTO 与集合只记录类型或规模，不记录密码、JWT、API Key 和完整消息正文。

## Embedding 模型

后端默认从 `hf-mirror.com` 下载 `all-MiniLM-L6-v2` ONNX 模型并缓存。如果容器网络无法访问镜像站，可把模型放入项目根目录的 `.model-cache/`：

```text
.model-cache/
  model.onnx
  tokenizer.json
```

然后在 `.env` 中指定：

```dotenv
EMBEDDING_MODEL_URI=file:/models/model.onnx
EMBEDDING_TOKENIZER_URI=file:/models/tokenizer.json
```

`docker-compose.yml` 会把 `.model-cache` 只读挂载到容器 `/models`。

## 安全与配置

- `src/main/resources/application.yml`、`.env`、`.model-cache/`、`frontend/node_modules/`、`frontend/dist/` 均不会提交到 Git。
- Docker Compose 使用环境变量注入密钥，不使用仓库内明文密钥。
- JWT 通过 `Authorization: Bearer <token>` 传递；登出后的 token 写入 Redis 黑名单。
- Swagger 页面公开，但业务接口仍按原 JWT 规则鉴权。

## 当前范围

- 商品图片支持 URL 输入，暂未实现文件上传。
- AI 会话仅保存在前端内存，暂未持久化 conversation/message。
- `SimpleVectorStore` 为内存向量库，重启后会重新构建。
- `all-MiniLM-L6-v2` 不是中文专用模型，中文规则检索为可用级别。
