# CampusAI Market 系统架构

```mermaid
flowchart LR
    Browser["浏览器<br/>Vue 3 SPA"]
    Nginx["Nginx<br/>静态资源 + /api 反向代理"]
    Backend["Spring Boot 3.5<br/>REST API / Security / AOP"]
    MySQL[("MySQL 8.0<br/>用户、商品、收藏、订单、AI 会话")]
    Redis[("Redis 7<br/>JWT 黑名单、热门/搜索缓存")]
    DeepSeek["DeepSeek API<br/>Chat + Tool Calling"]
    Embedding["本地 Embedding<br/>all-MiniLM-L6-v2 ONNX"]
    VectorStore["SimpleVectorStore<br/>交易规则向量检索"]
    ModelCache["模型缓存<br/>.model-cache"]
    Uploads["图片存储<br/>uploads_data"]

    Browser -->|"HTTP / HTTPS"| Nginx
    Nginx -->|"静态资源"| Browser
    Nginx -->|"/api"| Backend
    Backend -->|"MyBatis-Plus"| MySQL
    Backend -->|"Lettuce"| Redis
    Backend -->|"OpenAI 兼容协议"| DeepSeek
    Backend -->|"JPG / PNG / WebP / GIF"| Uploads
    Backend --> VectorStore
    VectorStore --> Embedding
    Embedding -->|"加载 ONNX / tokenizer"| ModelCache
```

## 关键链路

1. 本地开发时由 Vite 将 `/api` 代理到 Spring Boot；Docker Compose 中由 Nginx 反向代理。
2. 登录成功后签发 JWT，后续受保护接口通过 `Authorization: Bearer <token>` 鉴权。
3. 登出 token 以 SHA-256 摘要写入 Redis 黑名单；热门商品和 Agent 搜索使用 Redis 缓存，Redis 不可用时业务自动降级。
4. 商品写操作通过缓存版本号使旧搜索结果失效，避免新增、修改、下架、重新上架和订单取消后读到脏数据。
5. AI 请求通过 DeepSeek Tool Calling 调用真实 Java 方法查询 MySQL；交易规则问题使用本地 Embedding 在 `SimpleVectorStore` 中检索后注入上下文。RAG 在首次规则提问时懒加载，模型不可用时自动跳过规则增强而不影响主服务启动。
6. AI 会话与消息保存在 MySQL，只加载当前用户最近 10 条消息用于多轮理解，并提供历史会话查询。
7. Docker 环境把 `.model-cache` 挂载到 `/models` 并保持可写，既复用本地模型，也允许首次运行时缓存远程模型；上传目录挂载到 `uploads_data` 持久化。

[返回 README](../README.md)
