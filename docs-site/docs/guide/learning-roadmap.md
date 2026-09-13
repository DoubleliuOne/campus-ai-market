# 十二阶段学习路线

每阶段都按“学什么、对应代码、顺序、学完会什么、面试能答什么”组织。

## 第 1 阶段：理解项目

**学什么**

- 项目定位、业务功能、代码基线。
- 后端、前端、数据库、Redis 和 AI 的边界。

**代码**

```text
README.md
pom.xml
sql/init.sql
src/main/java/com/campusmarket/CampusaiMarketApplication.java
```

**顺序**

1. 自己用一句话介绍项目。
2. 画出七张表关系。
3. 列出七个 Controller。
4. 标注 AI 所在的模块。

**学完会什么**

不看代码能说出项目的三大块：交易业务、AI 能力、部署。

**面试能答**

> 项目整体做什么，为什么不是单纯聊天项目。

## 第 2 阶段：Spring Boot

**学什么**

- 启动类、自动配置、Bean、IOC、DI、AOP、事务。

**代码**

```text
CampusaiMarketApplication.java
config/
aspect/CallLoggingAspect.java
```

**顺序**

1. 从 main 方法追 Bean 扫描。
2. 选 `AuthController -> UserService -> UserMapper` 看依赖注入。
3. 看 AOP 日志。
4. 看 `@Transactional` 如何包裹订单。

**学完会什么**

解释为什么没有手写大量 `new` 和配置。

**面试能答**

> 自动配置做什么，IOC/DI 在项目哪里体现，事务为何依赖代理。

## 第 3 阶段：数据库

**学什么**

- 七张表、字段、外键、索引。
- Entity、VO、分页、条件构造器。

**代码**

```text
sql/init.sql
entity/
mapper/
service/ItemService.java
service/OrderService.java
```

**顺序**

1. 画 ER 图。
2. 手写商品搜索 SQL。
3. 解释每张表的数据来源。
4. 看 MyBatis-Plus 分页。

**学完会什么**

从一条查询追到具体 SQL 和结果映射。

**面试能答**

> 为什么用 MyBatis-Plus，表如何关联，分页如何实现。

## 第 4 阶段：JWT 与 Security

**学什么**

- 注册、BCrypt、登录、JWT 结构、Filter、登出黑名单。

**代码**

```text
security/
service/UserService.java
service/TokenBlacklistService.java
controller/AuthController.java
```

**顺序**

1. 画注册和登录时序图。
2. 手工解析一个 JWT Payload。
3. 走一遍 Filter。
4. 解释 Redis 黑名单 TTL。

**学完会什么**

能从 Authorization Header 讲到 SecurityContext。

**面试能答**

> 为什么 JWT、怎么验证、怎么登出、Redis 故障怎么办。

## 第 5 阶段：Redis

**学什么**

- Key、Value、TTL、缓存失效和降级。

**代码**

```text
service/ItemService.java
service/TokenBlacklistService.java
```

**顺序**

1. 背下四类 Key 格式。
2. 走热门和搜索缓存。
3. 画写后失效时序。
4. 判断三大缓存问题。

**学完会什么**

能解释 Redis 与 MySQL 的职责和数据一致性。

**面试能答**

> 缓存什么、何时写、何时删、缓存问题、Redis 挂了怎么办。

## 第 6 阶段：商品与订单业务

**学什么**

- 商品全生命周期。
- 订单状态机和并发。
- 收藏与文件。

**代码**

```text
controller/ItemController.java
service/ItemService.java
controller/OrderController.java
service/OrderService.java
service/OrderStatusPolicy.java
service/FavoriteService.java
service/FileStorageService.java
```

**顺序**

1. 画商品状态图。
2. 画订单状态图。
3. 追一次创建订单。
4. 列出买卖双方权限。

**学完会什么**

能独立修改商品状态或订单规则。

**面试能答**

> 防超卖、状态机、事务、权限和缓存失效。

## 第 7 阶段：Vue

**学什么**

- Vue 3、路由、Axios、登录状态、页面调用链。

**代码**

```text
frontend/src/api/
frontend/src/router/
frontend/src/stores/
frontend/src/views/
```

**顺序**

1. 从登录按钮追到后端。
2. 从商品卡片追到详情 API。
3. 从购买按钮追到订单接口。
4. 从 AI 发送按钮追到 AgentController。

**学完会什么**

前后端任意主要按钮都能追出完整链路。

**面试能答**

> Token 怎么带、401 怎么处理、路由守卫和后端权限区别。

## 第 8 阶段：Agent 与 Tool Calling

**学什么**

- ChatClient、Tool Schema、ToolContext、四个 Tool。

**代码**

```text
ai/service/AiChatService.java
ai/tool/ItemTools.java
ai/tool/OrderTools.java
ai/tool/ToolCallBudget.java
```

**顺序**

1. 写清普通 AI 与 Agent 区别。
2. 画 Tool Call 时序。
3. 逐个分析四个 Tool。
4. 解释用户身份如何传。

**学完会什么**

能准确回答“AI 怎么调用 Java”。

**面试能答**

> Tool 注册、Schema、模型输出、Spring AI 执行、结果回模型。

## 第 9 阶段：多轮会话

**学什么**

- conversation/message、最近 10 条、用户隔离和 Markdown。

**代码**

```text
service/AgentConversationService.java
entity/AiConversation.java
entity/AiMessage.java
mapper/AiConversationMapper.java
```

**顺序**

1. 画两表关系。
2. 追发送消息。
3. 解释为什么限制历史。
4. 测试另一个用户访问会话。

**学完会什么**

能解释会话持久化和上下文成本。

**面试能答**

> 为什么两张表、为什么 10 条、如何隔离用户、失败补偿。

## 第 10 阶段：RAG

**学什么**

- Embedding、向量、余弦相似度、SimpleVectorStore、Top 3。

**代码**

```text
ai/rag/KnowledgeBaseService.java
ai/rag/RagProperties.java
src/main/resources/knowledge/trading-rules.txt
```

**顺序**

1. 手工切开规则文档。
2. 画出文本到向量流程。
3. 解释 `topK` 和无阈值影响。
4. 对比 Agent 和 RAG。

**学完会什么**

能讲清 RAG 原理和当前项目局限。

**面试能答**

> Embedding、向量检索、为什么 RAG、为什么本地模型、如何优化。

## 第 11 阶段：Docker

**学什么**

- Dockerfile、Compose、Nginx、Volume、Network、Env、健康检查。

**代码**

```text
backend.Dockerfile
frontend/Dockerfile
frontend/nginx.conf
docker-compose.yml
application-docker.yml
```

**顺序**

1. 画四服务拓扑。
2. 说明两个多阶段构建。
3. 追 `/api` 代理。
4. 识别持久化和环境变量。

**学完会什么**

能重建并排查完整部署。

**面试能答**

> 项目怎么部署、镜像和容器区别、Volume 和代理。

## 第 12 阶段：源码复盘与面试模拟

**学什么**

- 源码地图、六大链路、高频追问和不足。

**代码**

全部核心模块，但通过问题驱动阅读。

**顺序**

1. 随机抽一个接口闭卷画出调用链。
2. 让同伴连续追问五层。
3. 找出三个真实改进点。
4. 现场修改一个小功能。

**学完会什么**

不依赖 Codex 讲清、修改和扩展项目。

**面试能答**

> 项目介绍、模块设计、核心难点、当前限制和优化方案。

## 80/20 复习法

时间少时优先：

1. AI Tool Calling。
2. RAG。
3. 订单并发状态机。
4. JWT + Redis 黑名单。
5. 商品缓存版本。
6. Vue 到 Controller 链路。

这六项几乎覆盖项目面试的主要追问。

继续阅读：[完成检查与当前状态](./completion.md)。
