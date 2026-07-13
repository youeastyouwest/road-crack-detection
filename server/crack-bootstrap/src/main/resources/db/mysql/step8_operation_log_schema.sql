-- ============================================
-- step8_operation_log_schema.sql
-- 操作审计日志表（仅建表，日志由系统运行时自动记录）
-- ============================================

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL,
    username VARCHAR(64) DEFAULT '',
    module VARCHAR(64) NOT NULL DEFAULT '',
    action VARCHAR(64) NOT NULL DEFAULT '',
    description VARCHAR(512) DEFAULT '',
    ip VARCHAR(64) DEFAULT '',
    params TEXT,
    status INT DEFAULT 1,
    error_msg VARCHAR(512) DEFAULT '',
    cost_time BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_operation_log_user_id (user_id),
    KEY idx_operation_log_module (module),
    KEY idx_operation_log_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
