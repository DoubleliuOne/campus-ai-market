# CampusAI Market

面向校园二手交易场景的 Java 后端 + Spring AI Agent 项目，用于学习 Java 后端开发并作为面试展示项目。

项目已经实现的业务包括：用户注册登录、商品发布/搜索/详情/修改/下架、收藏、订单流转；AI 能力包括基于 DeepSeek Tool Calling 的商品搜索、详情查询、订单查询、推荐，以及基于本地 Embedding 的交易规则 RAG 问答。

## 技术栈

- Java 17 / Spring Boot 3.5.4
- Spring AI 1.1.8（OpenAI 兼容接入 DeepSeek、本地 Transformers Embedding、SimpleVectorStore）
- MyBatis-Plus 3.5.9 / MySQL 8.0 / Redis 7
- Spring Security + JWT + BCrypt
- Vue 3 + Vite + Element Plus + Axios
- Docker Compose（MySQL、Redis、Backend、Frontend）

## 项目结构

```text
src/main/java/com/campusmarket/
  ai/       Agent Chat、RAG、Tool Calling
  controller/   REST API
  service/      业务逻辑
  mapper/       MyBatis-Plus Mapper
  dto/vo/entity/ 请求、响应、实体
  security/     JWT 与 Spring Security
sql/init.sql    建表与演示账号初始化
frontend/       Vue 3 前端
docker-compose.yml  一键部署配置
```

## Docker Compose 运行

1. 安装并启动 Docker Desktop。
2. 在项目根目录复制环境变量模板并填写：

```powershell
Copy-Item .env.example .env
```

需要填写：

- `DEEPSEEK_API_KEY`：DeepSeek API Key
- `JWT_SECRET`：至少 32 个随机字符
- `MYSQL_ROOT_PASSWORD`、`MYSQL_USER`、`MYSQL_PASSWORD`：可保留开发默认值，也可修改

3. 启动完整栈：

```powershell
docker compose up -d --build
```

4. 验证：

```powershell
docker compose ps
Invoke-RestMethod -Uri 'http://localhost:8081/' -Method Get
Invoke-RestMethod -Uri 'http://localhost:8080/api/categories' -Method Get
```

访问地址：

- 前端页面：`http://localhost:8081/`
- 后端 API：`http://localhost:8080/`

演示账号：

- 卖家/普通用户：`bob / 123456`
- 买家：`buyer1 / 123456`

## Embedding 模型说明

后端启动时会加载 `all-MiniLM-L6-v2` 的 ONNX 模型。默认从 `hf-mirror.com` 下载并缓存；如果容器网络无法访问该镜像站，可把模型放入项目根目录的 `.model-cache/`（该目录已被 Git 忽略），文件结构为：

```text
.model-cache/
  model.onnx
  tokenizer.json
```

并在 `.env` 中指定：

```dotenv
EMBEDDING_MODEL_URI=file:/models/model.onnx
EMBEDDING_TOKENIZER_URI=file:/models/tokenizer.json
```

`docker-compose.yml` 已将 `.model-cache` 只读挂载到容器 `/models`。

## 本地开发

本地运行前需要本机 MySQL（`campusai_market` 库）、Redis，并创建 `src/main/resources/application.yml`（含 MySQL 密码、DeepSeek Key、JWT Secret，该文件已被 Git 忽略）。

```powershell
# 后端
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd' -f 'D:\CampusAI Market\pom.xml' spring-boot:run

# 前端
cd 'D:\CampusAI Market\frontend'
npm install
npm run dev
```

本地前端默认地址为 `http://127.0.0.1:5173/`，Vite 会把 `/api` 代理到 `http://localhost:8080`。

## 注意

- 目录名含空格，PowerShell、Maven、Git、Docker 命令中的路径请使用引号。
- `src/main/resources/application.yml`、`.env`、`.model-cache/` 均不会提交到 Git。
- MySQL 与 Redis 容器不映射宿主机端口，避免与本机开发服务冲突。
