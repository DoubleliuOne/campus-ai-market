# 目录与知识地图

## 后端根目录

```text
src/main/java/com/campusmarket/
├── ai/
│   ├── rag/
│   ├── service/
│   └── tool/
├── aspect/
├── common/
├── config/
├── controller/
├── dto/
├── entity/
├── exception/
├── mapper/
├── security/
├── service/
└── vo/
```

## `ai/`：AI 能力边界

### `ai/service/AiChatService.java`

- **是什么**：AI 对话编排服务。
- **为什么存在**：集中处理 System Prompt、ChatClient、历史消息、商品上下文、RAG。
- **被谁调用**：`AgentController` 和 `AgentConversationService`。
- **调用谁**：`ChatClient`、`ItemService`、`KnowledgeBaseService`、`ItemTools`、`OrderTools`。
- **核心输入**：用户消息、`LoginUser`、可选 `itemId`、历史消息。
- **核心输出**：模型生成的 Markdown 字符串。

### `ai/tool/ItemTools.java`

- **是什么**：商品工具集合。
- **为什么存在**：把商品查询能力暴露给 LLM。
- **被谁调用**：Spring AI Tool Calling 机制。
- **调用谁**：`ItemService.search`、`ItemService.getDetailForUser`。
- **核心方法**：`searchItems`、`getItemDetail`、`recommendItems`。
- **输出**：JSON 字符串，交给模型读取。

### `ai/tool/OrderTools.java`

- **是什么**：订单查询工具。
- **调用谁**：`OrderService.myOrders`。
- **核心约束**：用户身份来自 `ToolContext`，不接受模型指定 `userId`。

### `ai/tool/ToolCallBudget.java`

- **是什么**：单次对话工具调用预算。
- **为什么存在**：限制模型反复调用 Tool，控制延迟和成本。
- **规则**：默认最多 6 次，使用 `AtomicInteger` 计数。

### `ai/rag/KnowledgeBaseService.java`

- **是什么**：平台规则 RAG 服务。
- **为什么存在**：用本地知识补充 DeepSeek 对项目规则的未知。
- **被谁调用**：`AiChatService.buildItemContext`。
- **调用谁**：TransformersEmbeddingModel、SimpleVectorStore。
- **关键策略**：命中规则关键词才加载和检索；失败时返回 `null`，不阻断 AI 主流程。

## `aspect/`：横切日志

`CallLoggingAspect` 通过 `@Around` 拦截 Controller 和 Service 公共方法：

- 记录类名、方法名、参数摘要、耗时和异常类型。
- 字符串只记录长度。
- `LoginUser` 只记录 userId。
- 不打印密码、JWT 或完整消息正文。

它不是业务逻辑，而是统一可观测性入口。

## `common/`：统一协议

| 文件 | 作用 | 谁使用 |
| --- | --- | --- |
| `ApiResponse<T>` | 统一响应 `{code,message,data}` | 所有 Controller、Security 异常响应 |
| `PageResult<T>` | 统一分页对象 | 商品、收藏、订单、AI 会话 |

## `config/`：Bean 与基础设施

| 文件 | 作用 |
| --- | --- |
| `AppConfig` | 注册 `BCryptPasswordEncoder` |
| `MybatisPlusConfig` | 注册 MySQL 分页拦截器 |
| `OpenApiConfig` | OpenAPI 元信息和 Bearer scheme |
| `StorageProperties` | 图片目录、大小和数量配置 |

## `controller/`：HTTP 入口

| Controller | 基础路径 | 主要职责 |
| --- | --- | --- |
| `AuthController` | `/api/auth` | 注册、登录、当前用户、登出 |
| `ItemController` | `/api/items` | 商品发布、搜索、详情、修改、上下架 |
| `FavoriteController` | `/api/favorites` | 收藏增删查 |
| `OrderController` | `/api/orders` | 创建订单、查询订单、状态流转 |
| `FileController` | `/api/files` | 上传和读取图片 |
| `CategoryController` | `/api/categories` | 分类列表 |
| `AgentController` | `/api/agent` | AI 对话和会话 CRUD |

Controller 只处理 HTTP 参数并转交给 Service，不应把核心业务规则散落在路由方法中。

## `dto/`：请求契约

DTO 表示进入系统的数据：

- 注册和登录：`RegisterRequest`、`LoginRequest`
- 商品发布和修改：`CreateItemRequest`、`UpdateItemRequest`
- 收藏：`FavoriteRequest`
- 订单：`CreateOrderRequest`、`UpdateOrderStatusRequest`
- AI：`AgentChatRequest`、`CreateConversationRequest`、`ConversationMessageRequest`

DTO 使用 Jakarta Validation 注解约束字段。

## `entity/`：数据库映射

| Entity | 表 |
| --- | --- |
| `User` | `user` |
| `Category` | `category` |
| `Item` | `item` |
| `Favorite` | `favorite` |
| `Order` | `orders` |
| `AiConversation` | `ai_conversation` |
| `AiMessage` | `ai_message` |

Entity 与数据库结构接近，不应该直接作为 API 输出。

## `mapper/`：持久层接口

大多数 Mapper 只继承 `BaseMapper<T>`：

```text
UserMapper
CategoryMapper
ItemMapper
FavoriteMapper
OrderMapper
AiConversationMapper
AiMessageMapper
```

两个自定义查询：

- `ItemMapper.selectByIdForUpdate`：创建订单时对商品加行锁。
- `AiConversationMapper.selectConversationPage`：一次查询会话及最后消息和消息数量。

## `security/`：认证与鉴权

| 文件 | 职责 |
| --- | --- |
| `JwtUtil` | 生成、校验、解析 JWT，计算剩余秒数 |
| `JwtAuthenticationFilter` | 从 Header 读取 JWT 并写入 SecurityContext |
| `SecurityConfig` | 定义公开接口、受保护接口和 401 JSON |
| `LoginUser` | Security 上下文中的当前用户，只含 id 和 username |

## `service/`：业务核心

| Service | 核心职责 |
| --- | --- |
| `UserService` | 注册、登录、登出 |
| `TokenBlacklistService` | Token 摘要黑名单 |
| `ItemService` | 商品业务和缓存 |
| `FavoriteService` | 收藏业务 |
| `OrderService` | 创建订单、订单查询、状态修改 |
| `OrderStatusPolicy` | 纯状态转换规则 |
| `FileStorageService` | 图片校验和本地存储 |
| `AgentConversationService` | AI 会话和消息持久化 |
| `CategoryService` | 分类列表 |

Service 是适合阅读项目业务的主要入口。大多数“为什么这么设计”的答案都在这里。

## `vo/`：响应契约

VO 表示返回给调用方的数据，通常会组合多个表：

- `ItemVO`：商品 + 卖家用户名 + 分类名
- `OrderVO`：订单 + 商品快照 + 买家/卖家用户名
- `LoginResponse`：Token 和基本用户信息
- `ConversationVO`：会话标题、最后消息、消息数量
- `ConversationMessageVO`：单条消息
- `UploadFileVO`：上传后的 URL、原始文件名和大小

这就是 Service 要把 Entity 转换为 VO 的原因。

## 前端目录

| 目录 | 作用 |
| --- | --- |
| `api/` | Axios 实例和 API 函数 |
| `router/` | 路由表、懒加载和登录守卫 |
| `stores/` | 轻量响应式登录状态 |
| `layouts/` | 顶部导航和页面框架 |
| `components/` | 商品卡片、图片、状态、空状态 |
| `views/` | 登录、首页、详情、发布、收藏、订单、AI |
| `utils/` | 时间、金额和状态格式化 |
| `styles/` | 全局设计变量和通用样式 |

## 从需求找一个文件

| 需求 | 第一入口 | 继续追 |
| --- | --- | --- |
| 修改登录规则 | `AuthController` | `UserService` |
| 修改商品搜索 | `ItemController.list` | `ItemService.search` |
| 修改购买规则 | `ItemDetailView.buyNow` | `OrderService.create` |
| 修改订单状态权限 | `OrdersView` | `OrderStatusPolicy` |
| 修改 AI 能查什么 | `ItemTools` / `OrderTools` | 对应 Service |
| 修改 RAG 知识 | `trading-rules.txt` | `KnowledgeBaseService` |
| 修改 JWT 放行路径 | `SecurityConfig` | Filter / JwtUtil |
| 修改容器端口 | `docker-compose.yml` | Dockerfile / Nginx |

下一步阅读：[Spring Boot 从零到项目](./spring-boot.md)。
