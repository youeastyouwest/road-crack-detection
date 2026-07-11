-- 预警记录表
CREATE TABLE IF NOT EXISTS `alert` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `alert_code` VARCHAR(64) NOT NULL COMMENT '预警编码',
    `alert_type` VARCHAR(32) NOT NULL COMMENT '预警类型：SEVERITY_DAMAGE/OVERDUE_WORKORDER/SUDDEN_CRACK',
    `alert_level` VARCHAR(16) NOT NULL COMMENT '预警等级：LOW/MEDIUM/HIGH',
    `title` VARCHAR(255) NOT NULL COMMENT '预警标题',
    `content` TEXT COMMENT '预警内容',
    `damage_type` VARCHAR(32) COMMENT '关联病害类型',
    `location` VARCHAR(255) COMMENT '位置信息',
    `work_order_id` BIGINT COMMENT '关联工单ID',
    `detection_task_id` BIGINT COMMENT '关联检测任务ID',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/HANDLED',
    `handled_by` VARCHAR(64) COMMENT '处理人',
    `handled_at` DATETIME COMMENT '处理时间',
    `handle_remark` VARCHAR(500) COMMENT '处理备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_alert_type` (`alert_type`),
    INDEX `idx_alert_level` (`alert_level`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警记录表';
