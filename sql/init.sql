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
    status      VARCHAR(20)     NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/PAID/COMPLETED/CANCELLED',
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
