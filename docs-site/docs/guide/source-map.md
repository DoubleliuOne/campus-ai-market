# 核心源码地图

本章按“类名、作用、调用关系、输入输出、对应业务”整理关键源码。

## 启动与配置

| 类/文件 | 作用 | 被谁调用 | 调用谁 | 核心输入/输出 |
| --- | --- | --- | --- | --- |
| `CampusaiMarketApplication` | 应用启动入口 | JVM | SpringApplication | main args / Spring 容器 |
| `AppConfig` | BCrypt Bean | Spring | BCrypt | 无 / PasswordEncoder |
| `MybatisPlusConfig` | 分页拦截器 | Spring/MyBatis | PaginationInnerInterceptor | 无 / 拦截器 Bean |
| `OpenApiConfig` | OpenAPI 信息 | SpringDoc | 无 | 注解元数据 |
| `StorageProperties` | 图片配置 | FileStorageService | 配置绑定 | properties / 上传属性 |
| `RagProperties` | RAG 配置 | KnowledgeBaseService | 配置绑定 | properties / URI 和目录 |
| `CallLoggingAspect` | Controller/Service 日志 | Spring AOP | JoinPoint | 方法调用 / 日志并继续调用 |

## 安全模块

| 类 | 作用 | 上游调用方 | 下游 | 输入 / 输出 |
| --- | --- | --- | --- | --- |
| `SecurityConfig` | 过滤链与路由权限 | Spring Security | JwtAuthenticationFilter | HttpSecurity / SecurityFilterChain |
| `JwtAuthenticationFilter` | 解析 JWT 并写 SecurityContext | Servlet Filter Chain | JwtUtil、TokenBlacklistService | Request / 已认证或匿名 Request |
| `JwtUtil` | 签发和解析 JWT | UserService、Filter、Blacklist | java-jwt | id/user / Token、LoginUser |
| `LoginUser` | 当前用户数据 | Controller、ToolContext | 无业务依赖 | id/username |
| `UserService` | 注册、登录、登出 | AuthController | UserMapper、BCrypt、JwtUtil | DTO / userId、LoginResponse |
| `TokenBlacklistService` | 黑名单 Key 和 TTL | Filter、UserService | Redis、JwtUtil | Token / blacklist 状态 |

## 商品模块

| 类 | 作用 | 上游 | 下游 | 输入 / 输出 |
| --- | --- | --- | --- | --- |
| `ItemController` | 商品 HTTP 接口 | Vue/HTTP | ItemService | DTO/参数 / ApiResponse |
| `ItemService` | 商品规则、查询、缓存、VO | Controller、ItemTools、OrderService | Mapper、Redis、ObjectMapper | id/条件 / ItemVO、PageResult |
| `ItemMapper` | 商品数据访问 | Service | MyBatis/MySQL | id/条件 / Item |
| `Item` | 商品表实体 | Mapper | 无 | 表字段 |
| `ItemVO` | 商品响应组合对象 | Service、Tool | JSON | 商品和关联字段 |

核心方法：

| 方法 | 做了什么 |
| --- | --- |
| `publish` | 校验内容、插入商品、清缓存 |
| `update` | 卖家与状态检查、条件更新、清缓存 |
| `takeOffShelf` | 在售商品改为下架 |
| `relist` | 下架商品恢复在售 |
| `getHotItems` | Redis 命中或查最近 10 件 |
| `search` | 动态分页查询和搜索缓存 |
| `myItems` | 当前卖家的商品分页 |
| `getDetailForUser` | 公开或卖家详情权限 |
| `toItemVOs` | 批量关联用户和分类，转 VO |

## 分类与收藏

| 类 | 作用 | 输入 / 输出 |
| --- | --- | --- |
| `CategoryController` | 分类列表接口 | 无 / `List<Category>` |
| `CategoryService` | 按 id 排序查分类 | 无 / `List<Category>` |
| `FavoriteController` | 收藏 API | itemId / 空或 ItemVO 列表 |
| `FavoriteService` | 收藏唯一性、查询、列表 | itemId + LoginUser / 状态或数据 |
| `FavoriteMapper` | 收藏 CRUD | Entity / DB |

## 订单模块

| 类 | 作用 | 上游 | 下游 |
| --- | --- | --- | --- |
| `OrderController` | 创建、查询、更新状态 | 前端、Agent 读取通过 Service | OrderService |
| `OrderService` | 事务、行锁、条件更新、VO | Controller、OrderTools | ItemMapper、OrderMapper、UserMapper |
| `OrderStatusPolicy` | 纯状态转换规则 | OrderService、测试 | BusinessException |
| `OrderMapper` | 订单 CRUD | Service | MySQL |

关键输入输出：

| 方法 | 输入 | 输出 |
| --- | --- | --- |
| `create` | CreateOrderRequest、LoginUser | orderId |
| `updateStatus` | orderId、目标状态、LoginUser | void |
| `myOrders` | 用户、role、page、size | `PageResult<OrderVO>` |

## 文件模块

| 类 | 作用 | 输入 / 输出 |
| --- | --- | --- |
| `FileController` | 上传和读取接口 | Multipart/文件名 / VO 或 Resource |
| `FileStorageService` | 文件头、MIME、路径和存储 | Multipart / UploadFileVO |
| `UploadFileVO` | 上传结果 | url、originalName、size |

## AI 模块

| 类 | 作用 | 上游 | 下游 |
| --- | --- | --- | --- |
| `AgentController` | AI 和会话 HTTP 接口 | Vue | AiChatService、ConversationService |
| `AiChatService` | Prompt、上下文、ChatClient | Controller、ConversationService | ChatClient、ItemService、RAG、Tools |
| `AgentConversationService` | 会话持久化、历史、用户隔离 | AgentController | Conversation/Message Mapper、AiChatService |
| `ItemTools` | 商品 Tool | Spring AI | ItemService |
| `OrderTools` | 订单 Tool | Spring AI | OrderService |
| `ToolCallBudget` | 工具调用次数 | Tools | 原子计数 |
| `KnowledgeBaseService` | RAG 检索 | AiChatService | Embedding、SimpleVectorStore、规则文档 |
| `RagProperties` | RAG 配置 | Spring/KnowledgeBaseService | URI、目录、开关 |

AI 核心方法：

| 方法 | 输入 | 输出 |
| --- | --- | --- |
| `AiChatService.chat` | message、LoginUser、itemId、history | Markdown 字符串 |
| `buildMessages` | 当前问题、历史、商品上下文 | Spring AI Message 列表 |
| `buildItemContext` | 消息、itemId、用户 | 注入 RAG/商品信息的 UserMessage |
| `ConversationService.sendMessage` | conversationId、message、用户 | AgentChatResponse |
| `loadHistory` | conversationId | 最近 10 条按时间排序消息 |
| `KnowledgeBaseService.retrieveRules` | question | Top 3 规则文本或 null |

## 通用与异常

| 类 | 作用 |
| --- | --- |
| `ApiResponse<T>` | 统一 `{code,message,data}` |
| `PageResult<T>` | 统一分页 |
| `BusinessException` | 业务错误，默认转 400 |
| `ServiceUnavailableException` | AI 等依赖不可用，转 503 |
| `GlobalExceptionHandler` | 所有异常到统一响应 |

## Entity 与表映射

| Entity | Table |
| --- | --- |
| `User` | `user` |
| `Category` | `category` |
| `Item` | `item` |
| `Favorite` | `favorite` |
| `Order` | `orders` |
| `AiConversation` | `ai_conversation` |
| `AiMessage` | `ai_message` |

## 前端核心文件

| 文件 | 职责 |
| --- | --- |
| `api/http.js` | Axios Token、解包、401 |
| `api/index.js` | API 函数 |
| `stores/auth.js` | 登录状态 |
| `router/index.js` | 路由与守卫 |
| `MainLayout.vue` | 顶部导航和页面框架 |
| `HomeView.vue` | 商品搜索与分页 |
| `ItemDetailView.vue` | 详情、收藏、购买 |
| `ItemFormView.vue` | 发布、编辑、图片上传 |
| `OrdersView.vue` | 买卖订单和状态按钮 |
| `AssistantView.vue` | 会话、消息、Markdown、Agent 上下文 |

## 测试地图

| 测试 | 覆盖点 |
| --- | --- |
| `JwtUtilTest` | 弱密钥、生成与解析 |
| `UserServiceTest` | 注册校验 |
| `ItemServiceTest` | 详情权限、搜索缓存版本 |
| `OrderStatusPolicyTest` | 状态机权限 |
| `FileStorageServiceTest` | PNG 上传、伪造 MIME |
| `AgentConversationServiceTest` | AI 空响应补偿 |
| `ToolCallBudgetTest` | 工具调用上限 |
| `KnowledgeBaseServiceTest` | RAG 降级、非规则跳过 |
| `GlobalExceptionHandlerWebTest` | 400/503 统一异常 |
| `UserMapperTest` | Spring 上下文可加载 |

## 如何自己扩展

新增一个普通接口：

1. 新增 DTO。
2. 定义 Controller 方法和路径。
3. 在 Service 写业务和权限。
4. 需要新表时新增 Entity、Mapper、SQL。
5. 返回 VO 而不是直接返回 Entity。
6. 补测试和 OpenAPI 注解。

新增一个 AI Tool：

1. 在现有或新 `@Component` Tool 类加 `@Tool` 方法。
2. 明确参数类型和说明。
3. 复用 Service，不直接写 SQL。
4. 从 ToolContext 获取当前用户，不接受 userId 参数。
5. 控制返回 JSON 大小。
6. 检查调用预算。
7. 注册到 `ChatClient.Builder.defaultTools`。

新增 RAG 文档：

1. 放入 `src/main/resources/knowledge/`。
2. 保持段落格式可被现有切分。
3. 修改 `loadRuleDocuments` 或改为更通用的加载逻辑。
4. 评估中文 Embedding 和检索效果。

继续阅读：[项目面试题库](./interview-questions.md)。
