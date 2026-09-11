# CampusAI Market ER 图

```mermaid
erDiagram
    USER ||--o{ ITEM : publishes
    USER ||--o{ FAVORITE : creates
    USER ||--o{ ORDERS : buys
    USER ||--o{ ORDERS : sells
    CATEGORY ||--o{ ITEM : classifies
    ITEM ||--o{ FAVORITE : receives
    ITEM ||--o{ ORDERS : generates

    USER {
        BIGINT id PK
        VARCHAR username UK
        VARCHAR password_hash
        VARCHAR campus
        DATETIME create_time
        DATETIME update_time
    }

    CATEGORY {
        BIGINT id PK
        VARCHAR name UK
        DATETIME create_time
    }

    ITEM {
        BIGINT id PK
        BIGINT seller_id FK
        BIGINT category_id FK
        VARCHAR title
        TEXT description
        DECIMAL price
        TEXT images
        VARCHAR status
        DATETIME create_time
        DATETIME update_time
    }

    FAVORITE {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT item_id FK
        DATETIME create_time
    }

    ORDERS {
        BIGINT id PK
        BIGINT buyer_id FK
        BIGINT seller_id FK
        BIGINT item_id FK
        DECIMAL price
        VARCHAR status
        DATETIME create_time
        DATETIME update_time
    }
```

## 状态说明

- `item.status`：`ON_SALE`、`SOLD`、`OFF_SHELF`
- `orders.status`：`CREATED`、`PAID`、`COMPLETED`、`CANCELLED`
- `favorite` 对 `(user_id, item_id)` 建立唯一约束，防止重复收藏。
- 金额字段使用 `DECIMAL(10,2)`，订单中的 `price` 是下单时的价格快照。

[返回 README](../README.md)
