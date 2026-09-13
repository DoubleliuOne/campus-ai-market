USE campusai_market;

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

ALTER TABLE orders
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CREATED'
    COMMENT 'CREATED/CONFIRMED/IN_PROGRESS/COMPLETED/CANCELLED';
