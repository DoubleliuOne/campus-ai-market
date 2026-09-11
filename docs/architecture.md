# CampusAI Market 系统架构

```mermaid
flowchart LR
    Browser["浏览器<br/>Vue 3 SPA"]
    Nginx["Nginx<br/>静态资源 + /api 反向代理"]
    Backend["Spring Boot 3.5<br/>REST API / Security / AOP"]
    MySQL[("MySQL 8.0<br/>用户、商品、收藏、订单")]
    Redis[("Redis 7<br/>JWT 黑名单、热门缓存")]
    DeepSeek["DeepSeek API<br/>Chat + Tool Calling"]
    Embedding["本地 Embedding<br/>all-MiniLM-L6-v2 ONNX"]
    VectorStore["SimpleVectorStore<br/>交易规则向量检索"]
    ModelCache["模型缓存<br/>.model-cache"]

    Browser -->|"HTTP / HTTPS"| Nginx
    Nginx -->|"静态资源"| Browser
    Nginx -->|"/api"| Backend
    Backend -->|"MyBatis-Plus"| MySQL
    Backend -->|"Lettuce"| Redis
    Backend -->|"OpenAI 兼容协议"| DeepSeek
    Backend --> VectorStore
    VectorStore --> Embedding
    Embedding -->|"加载 ONNX / tokenizer"| ModelCache
```

## 关键链路

1. 本地开发时由 Vite 将 `/api` 代理到 Spring Boot；Docker Compose 中由 Nginx 反向代理。
2. 登录成功后签发 JWT，后续受保护接口通过 `Authorization: Bearer <token>` 鉴权。
3. 登出 token 写入 Redis 黑名单；热门商品列表使用 Redis 缓存，Redis 不可用时业务自动降级。
4. AI 请求通过 DeepSeek Tool Calling 调用真实 Java 方法查询 MySQL；交易规则问题使用本地 Embedding 在 `SimpleVectorStore` 中检索后注入上下文。
5. Docker 环境可把 `.model-cache` 只读挂载到 `/models`，避免容器运行时下载模型。

[返回 README](../README.md)
