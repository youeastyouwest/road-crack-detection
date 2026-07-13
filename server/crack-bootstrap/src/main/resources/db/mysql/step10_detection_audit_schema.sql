-- 检测严重程度人工调整审计日志表
CREATE TABLE IF NOT EXISTS detection_severity_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '检测任务ID',
    result_id BIGINT NOT NULL COMMENT '检测结果ID',
    item_id BIGINT NULL COMMENT '病害项ID（批量调整时为NULL）',
    operator_id BIGINT NULL COMMENT '操作人用户ID',
    operator_name VARCHAR(64) NOT NULL COMMENT '操作人用户名',
    adjustment_type VARCHAR(20) NOT NULL COMMENT 'BATCH(批量)或ITEM(逐项)',
    original_severity VARCHAR(20) NOT NULL COMMENT '原严重程度',
    new_severity VARCHAR(20) NOT NULL COMMENT '新严重程度',
    reason VARCHAR(500) NULL COMMENT '调整原因（可选）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_task_id (task_id),
    KEY idx_audit_item_id (item_id),
    KEY idx_audit_operator_id (operator_id),
    KEY idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测严重程度人工调整审计日志';
