# 项目面试题库

每道题按“简单回答 → 结合项目 → 追问 → 追问回答 → 源码”组织。先闭卷口述，再展开看答案。

## Java 与 Spring Boot

### 1. 为什么选择 Java 17？

**简单回答**：Java 17 是长期支持版本，语言特性和生态都比较稳定。

**结合项目**：`pom.xml` 固定 `java.version=17`，代码使用 record、switch 表达式等能力，Spring Boot 3.5.4 也要求现代 JDK 基线。

**追问**：Spring Boot 3 为什么不能继续用 Java 8？

**追问回答**：Spring Boot 3 / Spring Framework 6 要求 Java 17 基线，同时迁移到 Jakarta 命名空间。项目中的 `jakarta.validation`、`jakarta.servlet` 就是这一代技术栈。

**源码**：`pom.xml`、`LoginUser.java`。

### 2. Spring Boot 自动配置在项目哪里体现？

**简单回答**：根据依赖和配置自动创建常用 Bean。

**结合项目**：Web Starter 自动配置 Spring MVC 和 Jackson，Security Starter 创建过滤链，Redis Starter 创建 `StringRedisTemplate`，MyBatis-Plus Starter 扫描 Mapper。

**追问**：项目覆盖了哪些默认行为？

**追问回答**：`SecurityConfig` 定义自己的无状态安全链，`MybatisPlusConfig` 注册分页拦截器，`AppConfig` 提供 BCrypt Bean，启动类还排除了默认 `UserDetailsServiceAutoConfiguration`。

**源码**：`CampusaiMarketApplication.java`、`config/`。

### 3. Bean、IOC、DI 在这个项目里是什么？

**简单回答**：Bean 是被 Spring 管理的对象，IOC 是创建控制权交给容器，DI 是容器注入依赖。

**结合项目**：`AuthController` 构造器注入 `UserService`；`UserService` 注入 Mapper、PasswordEncoder、JwtUtil；Tools 也由 Spring 管理。

**追问**：为什么使用构造器注入？

**追问回答**：依赖明确、对象创建后不可变、测试容易替换，也避免字段注入隐藏依赖。

**源码**：`AuthController.java`、`UserService.java`、`ItemTools.java`。

### 4. 为什么 Controller 不直接返回 Entity？

**简单回答**：Entity 面向上数据库，API 应返回稳定的展示契约。

**结合项目**：商品 API 返回 `ItemVO`，包含 sellerUsername 和 categoryName，这两个字段不在 Item 表中。User Entity 还包含 passwordHash，绝不能暴露。

**追问**：所有接口都必须用 VO 吗？

**追问回答**：不一定。分类接口当前直接返回 `Category`，因为字段少且没有敏感信息。但当需要组合字段或隐藏字段时，应使用 VO。

**源码**：`ItemVO.java`、`ItemService.toItemVOs`、`CategoryController.java`。

## Security 与 JWT

### 5. 为什么使用 JWT？

**简单回答**：实现无状态鉴权，适合前后端分离 API。

**结合项目**：服务端使用 `SessionCreationPolicy.STATELESS`，登录签发 JWT，后续 Filter 从 Header 验证并创建 `LoginUser`。

**追问**：JWT 有什么缺点？

**追问回答**：签发后无法天然撤回、Token 较大、Payload 可解码，所以项目增加 Redis 黑名单，并规定不放敏感数据。

**源码**：`JwtUtil.java`、`JwtAuthenticationFilter.java`、`SecurityConfig.java`。

### 6. JWT 的三部分是什么？

**简单回答**：Header、Payload、Signature。

**结合项目**：Payload 包含 `sub=userId`、username、iat、exp，Signature 由 32 字节以上 Secret 的 HMAC256 生成。

**追问**：Payload 能看到，为什么还安全？

**追问回答**：JWT 保证完整性和来源，不保证机密性。攻击者改 Payload 后无法生成正确签名，但任何拿到 Token 的人都能看到 Payload 内容。

**源码**：`JwtUtil.generateToken`、`JwtUtil.parseToken`。

### 7. 登出到底怎么实现？

**简单回答**：把 Token 加入服务端黑名单。

**结合项目**：`UserService.logout` 提取 Header，`TokenBlacklistService` 用 SHA-256 生成 Redis Key，Value 为 1，TTL 等于 Token 剩余有效期。

**追问**：为什么不存完整 Token？

**追问回答**：摘要固定长度、避免 Key 过长，并降低 Redis 运维界面暴露完整凭证的风险。相同 Token 的摘要一致，不影响判断。

**源码**：`TokenBlacklistService.java`。

### 8. Redis 挂了，JWT 还能用吗？

**简单回答**：还能做签名和过期校验，但黑名单失效。

**结合项目**：Filter 捕获 Redis 异常并继续验 JWT，避免 Redis 故障把所有合法请求打成 401。

**追问**：安全代价是什么？

**追问回答**：已经登出的 Token 在 Redis 恢复前可能重新可用。生产环境应使用 Redis 高可用、短 Token、Refresh Token 或服务端版本号降低风险。

**源码**：`JwtAuthenticationFilter.java`。

### 9. 注册密码如何保存？

**简单回答**：使用 BCrypt 哈希，不保存明文。

**结合项目**：`AppConfig` 注册 `BCryptPasswordEncoder`，`UserService.register` 调用 `encode`，登录调用 `matches`。

**追问**：为什么不用 MD5？

**追问回答**：MD5 快、无盐且易彩虹表攻击。BCrypt 自带随机盐，工作因子可调，更适合密码存储。

**源码**：`AppConfig.java`、`UserService.java`、`sql/init.sql`。

## MyBatis-Plus 与 MySQL

### 10. 为什么使用 MyBatis-Plus？

**简单回答**：简化 MyBatis 的通用 CRUD、条件查询和分页。

**结合项目**：Mapper 继承 `BaseMapper`，商品查询使用 `LambdaQueryWrapper`，订单和商品列表使用分页，并发场景保留自定义 SQL。

**追问**：为什么仍有 `@Select`？

**追问回答**：通用 CRUD 无法表达 `FOR UPDATE` 和复杂会话分页，所以保留自定义 SQL。MyBatis-Plus 不排斥 MyBatis 原生能力。

**源码**：`ItemMapper.java`、`AiConversationMapper.java`、`MybatisPlusConfig.java`。

### 11. 分页是怎么工作的？

**简单回答**：Page + Wrapper + PaginationInnerInterceptor。

**结合项目**：Service 创建 `Page` 和查询条件，MyBatis-Plus 分页拦截器生成总数和分页查询，Service 再转换成 `PageResult<ItemVO>`。

**追问**：为什么要限制 size？

**追问回答**：防止客户端一次请求过大结果集，Controller 和 Service 都限制最大 100。

**源码**：`ItemService.search`、`MybatisPlusConfig.java`。

### 12. 为什么订单要保存价格快照？

**简单回答**：订单价格必须固定，不随商品后续修改变化。

**结合项目**：`OrderService.create` 从当时读取的 Item 复制 `price` 到 Order。

**追问**：为什么没有保存商品标题快照？

**追问回答**：当前实现没有保存，订单查询时读商品当前标题。严格的历史订单设计应保存标题等快照，这是可改进点。

**源码**：`OrderService.create`、`OrderVO` 组装。

### 13. 数据库索引有哪些？

**简单回答**：围绕高频过滤和排序建立组合索引。

**结合项目**：商品有 `(seller_id,status)`、`(category_id,status)`、`(status,create_time)`；订单有买家、卖家和商品索引；收藏有用户商品唯一键。

**追问**：为什么 `favorite` 要唯一键？

**追问回答**：防止同一用户重复收藏同一商品。Service 有查重，也只能把数据库唯一约束作为并发下最终保障。

**源码**：`sql/init.sql`。

## Redis

### 14. 项目中 Redis 存了什么？

**简单回答**：JWT 黑名单、热门缓存、搜索版本和搜索结果。

**结合项目**：黑名单前缀 `auth:blacklist:v2:`，热门 Key `cache:item:hot`，搜索前缀 `cache:item:search:v1:`，另有 `:version`。

**追问**：哪个不是缓存？

**追问回答**：JWT 黑名单是安全状态，不是可任意删除的加速数据。虽然 Redis 故障时会降级，但正常运行时它是主动登出的关键。

**源码**：`TokenBlacklistService.java`、`ItemService.java`。

### 15. 缓存一致性怎么保证？

**简单回答**：数据库写成功后删热缓存或切换版本号。

**结合项目**：普通写操作调用 `evictItemCaches`，订单事务注册 `afterCommit`。搜索缓存不做双写，只让 Key 版本变化，旧键 TTL 90 秒。

**追问**：为什么不用更新缓存？

**追问回答**：更新缓存容易产生数据库与缓存双写一致性问题。删除或版本失效让下一次读取从数据库重建，逻辑更简单。

**源码**：`ItemService.evictItemCaches`、`evictItemCachesAfterCommit`。

### 16. 缓存穿透、击穿、雪崩有没有解决？

**简单回答**：部分缓解，没有全部实现。

**结合项目**：空搜索结果会被缓存，降低重复穿透；但没有布隆过滤器。热门 Key 没有互斥锁，存在击穿风险。搜索 Key 过期分散，雪崩风险较低。

**追问**：你会怎么优化？

**追问回答**：空值短 TTL、布隆过滤器、热点 Key 互斥重建或逻辑过期，再加 Redis 监控和降级。优化前先根据实际流量确定优先级。

**源码**：`ItemService.getHotItems`、`ItemService.search`。

## 订单

### 17. 如何防止并发超卖？

**简单回答**：事务内行锁加条件更新。

**结合项目**：`selectByIdForUpdate` 锁定商品，`UPDATE ... WHERE status='ON_SALE'` 只允许一次成功，订单插入也在同一事务。

**追问**：只有条件更新够不够？

**追问回答**：正常路径通常够，但还需要读取卖家、价格并创建订单，行锁让这一整段逻辑串行。两层一起提供更强保护。

**源码**：`OrderService.create`、`ItemMapper.selectByIdForUpdate`。

### 18. 订单状态机怎么做？

**简单回答**：用策略类集中验证当前状态、目标状态和角色。

**结合项目**：`OrderStatusPolicy` 规定卖家确认、推进和完成，买卖双方可在完成前取消，终态不可再变。更新时用旧状态做条件。

**追问**：为什么不用 if-else 写在 Controller？

**追问回答**：状态规则会复杂且需要测试。单独策略类便于复用、单测和修改，Controller 只处理协议。

**源码**：`OrderStatusPolicy.java`、`OrderService.updateStatus`。

### 19. 取消订单为什么要恢复商品？

**简单回答**：释放被订单占用的商品。

**结合项目**：下单即 `SOLD`，取消订单时把 `SOLD` 恢复为 `ON_SALE`，两者在同一事务。

**追问**：`IN_PROGRESS` 能取消吗？

**追问回答**：当前不能。代码只允许 `CREATED/CONFIRMED` 取消，进入交易中后由卖家完成。现实中应增加争议处理，但项目未实现。

**源码**：`OrderService.updateStatus`、`OrderStatusPolicy`。

## Vue

### 20. 前端如何保存和携带登录状态？

**简单回答**：localStorage 保存 Token，Axios 请求拦截器自动添加 Header。

**结合项目**：`stores/auth.js` 管理响应式状态和持久化，`http.js` 每次读取 Token，401 时清理并跳转登录。

**追问**：localStorage 有什么风险？

**追问回答**：容易受 XSS 影响。项目对 AI Markdown 使用 DOMPurify，但更严格的方案可考虑 HttpOnly Cookie、短 Token 和 CSP。

**源码**：`stores/auth.js`、`api/http.js`。

### 21. 路由守卫能代替后端权限吗？

**简单回答**：不能。

**结合项目**：路由守卫只阻止未登录页面访问；商品所有者、订单参与者、AI 会话所有者都由后端 Service 检查。

**追问**：举个越权例子。

**追问回答**：用户即使手工请求别人的 conversationId，`requireOwnedConversation` 仍检查 `user_id`；修改商品也检查 sellerId。

**源码**：`router/index.js`、`AgentConversationService.java`、`ItemService.java`。

### 22. 为什么 AI Markdown 要用 DOMPurify？

**简单回答**：防止模型输出不可信 HTML 引发 XSS。

**结合项目**：`marked` 把 Markdown 转 HTML，`DOMPurify.sanitize` 清理后再 `v-html`。

**追问**：只在后端过滤行不行？

**追问回答**：后端可以做内容策略，但前端最终把字符串解释为 HTML，浏览器侧清洗同样重要。两端防护并不冲突。

**源码**：`AssistantView.vue`。

## Docker

### 23. Dockerfile 和 Compose 分别做什么？

**简单回答**：Dockerfile 构建单个镜像，Compose 编排多个服务。

**结合项目**：后端和前端各有 Dockerfile；Compose 启动 MySQL、Redis、Backend、Frontend，并用健康检查控制依赖顺序。

**追问**：为什么前端也放 Docker？

**追问回答**：生产环境需要 Nginx 托管静态资源和代理 `/api`，把前端构建和运行环境固定下来，避免依赖开发机 Node。

**源码**：`backend.Dockerfile`、`frontend/Dockerfile`、`docker-compose.yml`。

### 24. `/api` 在容器中怎么转发？

**简单回答**：Nginx 反向代理到 Compose 服务名 backend。

**结合项目**：`proxy_pass http://backend:8080`，Docker DNS 把服务名解析为容器地址，前端不需要知道后端 IP。

**追问**：开发环境怎么转发？

**追问回答**：Vite Dev Server 的 `/api` proxy 转到 `http://localhost:8080`。生产构建不使用 Vite Server，所以需要 Nginx。

**源码**：`frontend/nginx.conf`、`vite.config.js`。

## Spring AI、Agent 与 Tool Calling

### 25. 普通 AI 和 Agent 有什么区别？

**简单回答**：Agent 可以决定调用工具，再把工具结果交给模型生成最终回答。

**结合项目**：ChatClient 注册了商品和订单 Tools，模型根据问题选择 `searchItems`、`getItemDetail`、`getMyOrders` 或 `recommendItems`。

**追问**：项目算真正的 Agent 吗？

**追问回答**：属于工具型 Agent 的最小实现。能自主调工具并继续生成，但没有复杂规划、多 Agent 协作或长期自主任务。

**源码**：`AiChatService.java`、`ItemTools.java`、`OrderTools.java`。

### 26. AI 到底怎么调用 Java？

**简单回答**：LLM 只生成 Tool Call，Spring AI 负责执行 Java 方法。

**结合项目**：`@Tool` 和 `@ToolParam` 生成 Tool Schema，模型输出工具名和 JSON 参数，Spring AI 匹配并执行 Java，结果再发回模型。

**追问**：用户身份怎么传？

**追问回答**：Controller 从 JWT 得到 LoginUser，AiChatService 把 userId 写入 ToolContext，Tool 方法从 ToolContext 读取。模型没有 userId 参数可伪造。

**源码**：`AiChatService.chat`、`ItemTools.currentUser`、`OrderTools.getMyOrders`。

### 27. Tool 返回什么？

**简单回答**：返回模型可读的 JSON 字符串。

**结合项目**：商品返回 `PageResult<ItemVO>` JSON，订单返回 `PageResult<OrderVO>` JSON，业务异常转为可读字符串。

**追问**：为什么不返回 Entity？

**追问回答**：Entity 可能含内部字段且缺少关联展示字段。VO 已满足接口和模型需要，也避免直接暴露数据库结构。

**源码**：`ItemTools.java`、`OrderTools.java`。

### 28. 如何控制 Tool 调用成本？

**简单回答**：限制调用次数和返回规模。

**结合项目**：每次 AI 请求创建 `ToolCallBudget`，默认最多 6 次。search 默认 5 条、最多 20 条，recommend 固定 10 条。

**追问**：调用超了怎么处理？

**追问回答**：Tool 返回“调用次数已达上限，请根据已有结果继续回答”，让模型使用已有信息收尾。

**源码**：`ToolCallBudget.java`、`ItemTools.java`。

## RAG

### 29. RAG 是什么，项目怎么实现？

**简单回答**：先检索相关知识，再让模型生成答案。

**结合项目**：规则文档按 `##` 切分，Embedding 后存 `SimpleVectorStore`。规则问题命中关键词后取 Top 3，注入当前用户消息，再由 DeepSeek 回答。

**追问**：为什么不是关键词搜索？

**追问回答**：Embedding 可以比较语义相似度，用户问题和文档不一定使用相同词。不过当前仍有规则关键词门控，门控本身是字符串匹配。

**源码**：`KnowledgeBaseService.java`、`trading-rules.txt`。

### 30. 为什么同时用 Agent 和 RAG？

**简单回答**：Agent 查实时业务，RAG 查稳定规则。

**结合项目**：商品和订单需要 MySQL、Redis 和用户隔离，适合 Tool；禁售、发布、退款是文本规则，适合 RAG。两者可以在同一请求组合。

**追问**：RAG 会不会泄露订单？

**追问回答**：当前订单不会进入知识库，RAG 只加载 `trading-rules.txt`。订单通过按 JWT 用户过滤的 Tool 查询，不存在公共向量库传播问题。

**源码**：`AiChatService.buildItemContext`、`KnowledgeBaseService`、`OrderTools`。

## 最后一问

### 31. 如果重新设计这个项目，你会先改什么？

**简单回答**：优先改可验证性、数据一致性和 AI 失败处理，而不是先加功能。

**结合项目**：

1. 为创建订单增加集成测试和数据库级约束。
2. 为 RAG 建立中文检索评测集和最低相似度阈值。
3. 为 AI Tool Call 增加脱敏轨迹和可观测性。
4. 把图片迁移到对象存储并清理孤立文件。
5. 生产化配置使用 Secrets、TLS、备份和监控。

**追问**：为什么不直接换微服务和向量数据库？

**追问回答**：当前规模下单体和小知识库不是主要瓶颈。先解决正确性、安全性和可测试性，收益更直接。微服务和向量数据库会增加运维复杂度，应由实际规模触发。

继续阅读：[高频追问树与质疑](./interview-challenges.md)。
