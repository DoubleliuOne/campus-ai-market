SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS campusai_market
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campusai_market;

-- User table: seller and buyer are both users
CREATE TABLE IF NOT EXISTS `user` (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    username      VARCHAR(50)     NOT NULL COMMENT 'login username',
    password_hash VARCHAR(100)    NOT NULL COMMENT 'BCrypt password hash',
    campus        VARCHAR(100)    NULL COMMENT 'campus name',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'platform users';

-- Demo accounts used by local development and Docker deployment
INSERT IGNORE INTO `user` (username, password_hash, campus)
VALUES ('bob', '$2a$10$7YCxPkmt80zKWVrxhmaDB.9zK6W21B85rhap6MXZokolbrhAV0AqS', '测试大学'),
       ('buyer1', '$2a$10$I7EFUJdjeooToU1nqNX1MO9GpCuAdr3TkQvky3MTu0qU/FO5e2ymK', '测试大学');

-- Category table: item classification
CREATE TABLE IF NOT EXISTS category (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    name        VARCHAR(50)     NOT NULL COMMENT 'category name',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'item categories';

INSERT IGNORE INTO category (name)
VALUES ('数码产品'),
       ('教材'),
       ('宿舍用品'),
       ('生活用品'),
       ('其他');

-- Item table: products published by sellers
CREATE TABLE IF NOT EXISTS item (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    seller_id   BIGINT UNSIGNED NOT NULL COMMENT 'seller user id',
    category_id BIGINT UNSIGNED NOT NULL COMMENT 'category id',
    title       VARCHAR(100)    NOT NULL COMMENT 'item title',
    description TEXT            NULL COMMENT 'item description',
    price       DECIMAL(10, 2)  NOT NULL COMMENT 'price in yuan',
    images      TEXT            NULL COMMENT 'image URLs as JSON array',
    status      VARCHAR(20)     NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE/SOLD/OFF_SHELF',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_item_seller_status (seller_id, status),
    KEY idx_item_category_status (category_id, status),
    KEY idx_item_status_create_time (status, create_time),
    CONSTRAINT fk_item_seller FOREIGN KEY (seller_id) REFERENCES `user` (id),
    CONSTRAINT fk_item_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'second-hand items';

-- Favorite table: buyer favorites an item
CREATE TABLE IF NOT EXISTS favorite (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT 'favorite owner id',
    item_id     BIGINT UNSIGNED NOT NULL COMMENT 'favorite item id',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_user_item (user_id, item_id),
    KEY idx_favorite_item (item_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_favorite_item FOREIGN KEY (item_id) REFERENCES item (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'user item favorites';

-- Orders table: buyer creates an order for an item
CREATE TABLE IF NOT EXISTS orders (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    buyer_id    BIGINT UNSIGNED NOT NULL COMMENT 'buyer user id',
    seller_id   BIGINT UNSIGNED NOT NULL COMMENT 'seller user id',
    item_id     BIGINT UNSIGNED NOT NULL COMMENT 'item id',
    price       DECIMAL(10, 2)  NOT NULL COMMENT 'order price snapshot',
    status      VARCHAR(20)     NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/CONFIRMED/IN_PROGRESS/COMPLETED/CANCELLED',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_orders_buyer (buyer_id, status),
    KEY idx_orders_seller (seller_id, status),
    KEY idx_orders_item (item_id),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES `user` (id),
    CONSTRAINT fk_orders_seller FOREIGN KEY (seller_id) REFERENCES `user` (id),
    CONSTRAINT fk_orders_item FOREIGN KEY (item_id) REFERENCES item (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'purchase orders';

-- AI conversation history, isolated by user
CREATE TABLE IF NOT EXISTS ai_conversation (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT 'conversation owner',
    title       VARCHAR(60)     NOT NULL DEFAULT '新对话' COMMENT 'conversation title',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_conversation_user_update (user_id, update_time),
    CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'AI conversation sessions';

-- Messages belonging to an AI conversation
CREATE TABLE IF NOT EXISTS ai_message (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    conversation_id BIGINT UNSIGNED NOT NULL COMMENT 'conversation id',
    role            VARCHAR(20)     NOT NULL COMMENT 'USER/ASSISTANT',
    content         TEXT            NOT NULL COMMENT 'message content',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_message_conversation_time (conversation_id, create_time),
    CONSTRAINT fk_ai_message_conversation
        FOREIGN KEY (conversation_id) REFERENCES ai_conversation (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'AI conversation messages';
