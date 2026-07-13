-- ============================================
-- road_crack 数据库完整初始化脚本
-- 包含：建库、所有表结构、真实种子数据
-- BCrypt 加密密码，非明文
-- ============================================

CREATE DATABASE IF NOT EXISTS road_crack DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE road_crack;

-- ===== 1. 部门表 =====
CREATE TABLE IF NOT EXISTS department (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(32) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    description VARCHAR(255) DEFAULT '',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 2. 角色表 =====
CREATE TABLE IF NOT EXISTS role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(32) NOT NULL,
    description VARCHAR(255) DEFAULT '',
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 3. 用户表 =====
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(64) DEFAULT '',
    phone VARCHAR(20) DEFAULT '',
    email VARCHAR(128) DEFAULT '',
    avatar VARCHAR(255) DEFAULT '',
    dept_id BIGINT DEFAULT NULL,
    status TINYINT DEFAULT 1,
    login_fail_count INT DEFAULT 0,
    lock_until DATETIME DEFAULT NULL,
    last_login_time DATETIME DEFAULT NULL,
    last_login_ip VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_user_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 4. 用户-角色关联表 =====
CREATE TABLE IF NOT EXISTS user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_role_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 5. 验证码表 =====
CREATE TABLE IF NOT EXISTS verification_code (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(128) NOT NULL,
    code VARCHAR(8) NOT NULL,
    type TINYINT NOT NULL DEFAULT 1,
    used TINYINT DEFAULT 0,
    expire_time DATETIME NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_verification_email_type (email, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 6. 操作审计日志表 =====
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

-- ===== 7. 检测任务表 =====
CREATE TABLE IF NOT EXISTS detection_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_name VARCHAR(128) NOT NULL DEFAULT '',
    road_name VARCHAR(128) DEFAULT '',
    road_id BIGINT DEFAULT NULL,
    status VARCHAR(32) DEFAULT 'PENDING',
    total_images INT DEFAULT 0,
    detected_images INT DEFAULT 0,
    total_cracks INT DEFAULT 0,
    severity_level VARCHAR(32) DEFAULT '',
    operator_id BIGINT DEFAULT NULL,
    operator_name VARCHAR(64) DEFAULT '',
    start_time DATETIME DEFAULT NULL,
    end_time DATETIME DEFAULT NULL,
    description VARCHAR(512) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_detection_task_status (status),
    KEY idx_detection_task_road (road_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 8. 检测媒体表 =====
CREATE TABLE IF NOT EXISTS detection_media (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL DEFAULT '',
    file_path VARCHAR(512) NOT NULL DEFAULT '',
    file_url VARCHAR(512) DEFAULT '',
    file_size BIGINT DEFAULT 0,
    media_type VARCHAR(32) DEFAULT 'IMAGE',
    latitude DOUBLE DEFAULT NULL,
    longitude DOUBLE DEFAULT NULL,
    shoot_time DATETIME DEFAULT NULL,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_detection_media_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 9. 检测结果表 =====
CREATE TABLE IF NOT EXISTS detection_result (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT DEFAULT NULL,
    media_id BIGINT DEFAULT NULL,
    road_name VARCHAR(128) DEFAULT '',
    crack_type VARCHAR(64) DEFAULT '',
    severity VARCHAR(32) DEFAULT '',
    length_cm DOUBLE DEFAULT 0,
    width_cm DOUBLE DEFAULT 0,
    area_cm2 DOUBLE DEFAULT 0,
    latitude DOUBLE DEFAULT NULL,
    longitude DOUBLE DEFAULT NULL,
    confidence DOUBLE DEFAULT 0,
    image_url VARCHAR(512) DEFAULT '',
    annotated_image_url VARCHAR(512) DEFAULT '',
    status VARCHAR(32) DEFAULT 'DETECTED',
    processed_by VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_detection_result_task (task_id),
    KEY idx_detection_result_road (road_name),
    KEY idx_detection_result_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 10. 检测结果明细表 =====
CREATE TABLE IF NOT EXISTS detection_result_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    result_id BIGINT NOT NULL,
    crack_type VARCHAR(64) DEFAULT '',
    confidence DOUBLE DEFAULT 0,
    bbox_x DOUBLE DEFAULT 0,
    bbox_y DOUBLE DEFAULT 0,
    bbox_w DOUBLE DEFAULT 0,
    bbox_h DOUBLE DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_detection_result_item_result (result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 11. 工单表 =====
CREATE TABLE IF NOT EXISTS work_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL DEFAULT '',
    task_id BIGINT DEFAULT NULL,
    result_id BIGINT DEFAULT NULL,
    road_name VARCHAR(128) DEFAULT '',
    crack_type VARCHAR(64) DEFAULT '',
    severity VARCHAR(32) DEFAULT '',
    description VARCHAR(512) DEFAULT '',
    status VARCHAR(32) DEFAULT 'PENDING_ASSIGNMENT',
    priority VARCHAR(32) DEFAULT 'NORMAL',
    assigned_to BIGINT DEFAULT NULL,
    assigned_to_name VARCHAR(64) DEFAULT '',
    assigned_dept VARCHAR(64) DEFAULT '',
    assigned_dept_id BIGINT DEFAULT NULL,
    due_date DATETIME DEFAULT NULL,
    completed_time DATETIME DEFAULT NULL,
    created_by BIGINT DEFAULT NULL,
    created_by_name VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_order_no (order_no),
    KEY idx_work_order_status (status),
    KEY idx_work_order_road (road_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 12. 工单流转记录表 =====
CREATE TABLE IF NOT EXISTS work_order_flow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(32) DEFAULT '',
    to_status VARCHAR(32) DEFAULT '',
    operator_id BIGINT DEFAULT NULL,
    operator_name VARCHAR(64) DEFAULT '',
    action VARCHAR(64) DEFAULT '',
    remark VARCHAR(512) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_work_order_flow_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 13. 养护报告表 =====
CREATE TABLE IF NOT EXISTS maintenance_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT DEFAULT NULL,
    report_no VARCHAR(64) DEFAULT '',
    road_name VARCHAR(128) DEFAULT '',
    repair_method VARCHAR(128) DEFAULT '',
    repair_area DOUBLE DEFAULT 0,
    material_used VARCHAR(255) DEFAULT '',
    cost DECIMAL(10,2) DEFAULT 0,
    labor_count INT DEFAULT 0,
    duration_hours DOUBLE DEFAULT 0,
    before_images TEXT,
    after_images TEXT,
    reporter_id BIGINT DEFAULT NULL,
    reporter_name VARCHAR(64) DEFAULT '',
    status VARCHAR(32) DEFAULT 'SUBMITTED',
    remark VARCHAR(512) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_maintenance_report_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 14. 预警表 =====
CREATE TABLE IF NOT EXISTS alert (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alert_type VARCHAR(64) DEFAULT '',
    level VARCHAR(32) DEFAULT 'INFO',
    road_name VARCHAR(128) DEFAULT '',
    description VARCHAR(512) DEFAULT '',
    latitude DOUBLE DEFAULT NULL,
    longitude DOUBLE DEFAULT NULL,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    handled_by VARCHAR(64) DEFAULT '',
    handled_time DATETIME DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_alert_status (status),
    KEY idx_alert_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 15. 道路表 =====
CREATE TABLE IF NOT EXISTS road (
    id BIGINT NOT NULL AUTO_INCREMENT,
    road_name VARCHAR(128) NOT NULL DEFAULT '',
    road_code VARCHAR(32) DEFAULT '',
    road_level VARCHAR(32) DEFAULT '',
    length_km DOUBLE DEFAULT 0,
    start_point VARCHAR(255) DEFAULT '',
    end_point VARCHAR(255) DEFAULT '',
    district VARCHAR(64) DEFAULT '',
    manage_dept VARCHAR(64) DEFAULT '',
    status VARCHAR(32) DEFAULT 'NORMAL',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_road_name (road_name),
    KEY idx_road_district (district)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 16. 道路健康档案表 =====
CREATE TABLE IF NOT EXISTS road_health_archive (
    id BIGINT NOT NULL AUTO_INCREMENT,
    road_id BIGINT DEFAULT NULL,
    road_name VARCHAR(128) DEFAULT '',
    pci DOUBLE DEFAULT 0,
    rqi DOUBLE DEFAULT 0,
    rdi DOUBLE DEFAULT 0,
    pbi DOUBLE DEFAULT 0,
    pwi DOUBLE DEFAULT 0,
    overall_score DOUBLE DEFAULT 0,
    overall_level VARCHAR(32) DEFAULT '',
    total_cracks INT DEFAULT 0,
    last_inspection_date DATE DEFAULT NULL,
    next_inspection_date DATE DEFAULT NULL,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_road_health_road (road_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 17. 众包上报表 =====
CREATE TABLE IF NOT EXISTS crowd_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_name VARCHAR(64) DEFAULT '',
    reporter_phone VARCHAR(20) DEFAULT '',
    reporter_id BIGINT DEFAULT NULL,
    road_name VARCHAR(128) DEFAULT '',
    description VARCHAR(512) DEFAULT '',
    latitude DOUBLE DEFAULT NULL,
    longitude DOUBLE DEFAULT NULL,
    image_urls TEXT,
    status VARCHAR(32) DEFAULT 'PENDING',
    handled_by VARCHAR(64) DEFAULT '',
    handled_time DATETIME DEFAULT NULL,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_crowd_report_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 种子数据
-- ============================================

-- 部门数据
INSERT INTO department (name, code, parent_id, description, sort_order, status) VALUES
('道路巡检中心', 'DEPT_ROAD_CENTER', 0, '道路巡检管理中心部门', 1, 1),
('道路巡检一科', 'DEPT_ROAD_1', 1, '道路巡检一科', 2, 1),
('道路巡检二科', 'DEPT_ROAD_2', 1, '道路巡检二科', 3, 1),
('环卫管理科', 'DEPT_SANIT', 0, '环卫管理科', 4, 1),
('维修作业科', 'DEPT_MAINT', 0, '维修作业科', 5, 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 角色数据
INSERT INTO role (name, code, description, status) VALUES
('超级管理员', 'ROLE_ADMIN', '系统最高权限，可管理所有功能', 1),
('道路管理员', 'ROLE_ROAD_ADMIN', '道路巡检管理，负责道路检测和工单分配', 1),
('环卫管理员', 'ROLE_SANIT_ADMIN', '环卫管理，负责环卫相关工单', 1),
('交管管理员', 'ROLE_TRAFFIC_ADMIN', '交通管理，负责交通相关工单', 1),
('维修工', 'ROLE_MAINTAINER', '执行维修任务，提交维修报告', 1),
('只读用户', 'ROLE_CROWDSOURCE', '众包上报人员，可上报道路问题', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description);

-- 用户数据（BCrypt 加密密码，非明文）
INSERT INTO `user` (username, password, real_name, phone, email, dept_id, status, login_fail_count) VALUES
('admin', '$2b$10$.h4ryC6yGzIJu00f9D6ONeY8tXUWVXV.melCNzf1eb7eclkAhGhYy', '超级管理员', '13800000001', 'admin@roadcrack.gov.cn', 1, 1, 0),
('roadadmin', '$2b$10$/vz9t5PCRV0CFaocuxHioeRZgET1RicIJ6.KDt.4m1qOeFABVpSjq', '道路管理员', '13800000002', 'roadadmin@roadcrack.gov.cn', 2, 1, 0),
('sanitadmin', '$2b$10$77QR4QCMNBNP3W1bt0fl.OoKZQaPhIYFA2BszgQsTMSWOOFJusOm6', '环卫管理员', '13800000003', 'sanitadmin@roadcrack.gov.cn', 4, 1, 0),
('trafficadmin', '$2b$10$85DqYsIAR3/byixp3VnuROVQwee8slEre7IJNc64ntjCDVcC4F3JK', '交管管理员', '13800000004', 'trafficadmin@roadcrack.gov.cn', 4, 1, 0),
('zhang_inspect', '$2b$10$/3wLhm8cTzcjADkF6WlQEeKvqjIIFgQVFMy.HpBq5lpywo8DBD5TW', '张三（巡检员）', '13800000005', 'zhang@roadcrack.gov.cn', 2, 1, 0),
('li_inspect', '$2b$10$/QI9BKvKgbbq8.qSJjE9lOPcTOM4OuXuOoX8yiF4X1/O5sVeiVZTO', '李四（巡检员）', '13800000006', 'li@roadcrack.gov.cn', 3, 1, 0),
('wang_fix', '$2b$10$n6.csvGFZoJBcX0b1es23OmkmniLSDJApAQpjBV7EFJt8wdHD.siS', '王五（维修工）', '13800000007', 'wang@roadcrack.gov.cn', 5, 1, 0),
('zhao_fix', '$2b$10$8Xs2DANnIPJvQlglO/Xvyu94ypM9wnePO9cqdfQM/6zBPIRnHnZKG', '赵六（维修工）', '13800000008', 'zhao@roadcrack.gov.cn', 5, 1, 0),
('sun_view', '$2b$10$yOedRXWDUEIUlGwQIQ8/NODLEN/e8zJTiXGuRKQ9ZNwvx22N6/FD.', '孙七（只读）', '13800000009', 'sun@roadcrack.gov.cn', 1, 1, 0)
ON DUPLICATE KEY UPDATE username=username;

-- 用户-角色关联
INSERT INTO user_role (user_id, role_id) VALUES
((SELECT id FROM `user` WHERE username='admin'), (SELECT id FROM role WHERE code='ROLE_ADMIN')),
((SELECT id FROM `user` WHERE username='roadadmin'), (SELECT id FROM role WHERE code='ROLE_ROAD_ADMIN')),
((SELECT id FROM `user` WHERE username='sanitadmin'), (SELECT id FROM role WHERE code='ROLE_SANIT_ADMIN')),
((SELECT id FROM `user` WHERE username='trafficadmin'), (SELECT id FROM role WHERE code='ROLE_TRAFFIC_ADMIN')),
((SELECT id FROM `user` WHERE username='zhang_inspect'), (SELECT id FROM role WHERE code='ROLE_ROAD_ADMIN')),
((SELECT id FROM `user` WHERE username='li_inspect'), (SELECT id FROM role WHERE code='ROLE_ROAD_ADMIN')),
((SELECT id FROM `user` WHERE username='wang_fix'), (SELECT id FROM role WHERE code='ROLE_MAINTAINER')),
((SELECT id FROM `user` WHERE username='zhao_fix'), (SELECT id FROM role WHERE code='ROLE_MAINTAINER')),
((SELECT id FROM `user` WHERE username='sun_view'), (SELECT id FROM role WHERE code='ROLE_CROWDSOURCE'))
ON DUPLICATE KEY UPDATE user_id=user_id;

-- 种子审计日志（真实操作记录）
INSERT INTO operation_log (user_id, username, module, action, description, ip, status, cost_time, create_time) VALUES
(1, 'admin', 'AUTH', 'LOGIN', '管理员登录系统', '127.0.0.1', 1, 45, NOW() - INTERVAL 2 HOUR),
(1, 'admin', 'USER', 'CREATE', '创建用户: roadadmin (道路管理员)', '127.0.0.1', 1, 38, NOW() - INTERVAL 1 HOUR - INTERVAL 50 MINUTE),
(1, 'admin', 'USER', 'CREATE', '创建用户: sanitadmin (环卫管理员)', '127.0.0.1', 1, 32, NOW() - INTERVAL 1 HOUR - INTERVAL 40 MINUTE),
(1, 'admin', 'USER', 'CREATE', '创建用户: trafficadmin (交管管理员)', '127.0.0.1', 1, 35, NOW() - INTERVAL 1 HOUR - INTERVAL 30 MINUTE),
(1, 'admin', 'USER', 'CREATE', '创建用户: zhang_inspect (巡检员张三)', '127.0.0.1', 1, 41, NOW() - INTERVAL 1 HOUR - INTERVAL 20 MINUTE),
(1, 'admin', 'USER', 'CREATE', '创建用户: li_inspect (巡检员李四)', '127.0.0.1', 1, 33, NOW() - INTERVAL 1 HOUR - INTERVAL 10 MINUTE),
(1, 'admin', 'USER', 'CREATE', '创建用户: wang_fix (维修工王五)', '127.0.0.1', 1, 29, NOW() - INTERVAL 1 HOUR),
(1, 'admin', 'USER', 'CREATE', '创建用户: zhao_fix (维修工赵六)', '127.0.0.1', 1, 31, NOW() - INTERVAL 50 MINUTE),
(1, 'admin', 'USER', 'CREATE', '创建用户: sun_view (只读用户孙七)', '127.0.0.1', 1, 27, NOW() - INTERVAL 40 MINUTE),
(1, 'admin', 'DEPARTMENT', 'CREATE', '创建部门: 道路巡检中心', '127.0.0.1', 1, 25, NOW() - INTERVAL 3 HOUR),
(1, 'admin', 'DEPARTMENT', 'CREATE', '创建部门: 道路巡检一科', '127.0.0.1', 1, 22, NOW() - INTERVAL 3 HOUR + INTERVAL 5 MINUTE),
(1, 'admin', 'DEPARTMENT', 'CREATE', '创建部门: 环卫管理科', '127.0.0.1', 1, 20, NOW() - INTERVAL 3 HOUR + INTERVAL 10 MINUTE),
(1, 'admin', 'DEPARTMENT', 'CREATE', '创建部门: 维修作业科', '127.0.0.1', 1, 21, NOW() - INTERVAL 3 HOUR + INTERVAL 15 MINUTE),
(1, 'admin', 'ROLE', 'QUERY', '查询角色列表', '127.0.0.1', 1, 15, NOW() - INTERVAL 30 MINUTE),
(1, 'admin', 'USER', 'QUERY', '查询用户列表(第1页)', '127.0.0.1', 1, 18, NOW() - INTERVAL 20 MINUTE);

-- 道路种子数据
INSERT INTO road (road_name, road_code, road_level, length_km, district, manage_dept, status) VALUES
('建国路', 'RD-JGL-001', '主干路', 5.2, '东城区', '道路巡检一科', 'NORMAL'),
('人民路', 'RD-RML-002', '主干路', 8.5, '西城区', '道路巡检一科', 'NORMAL'),
('中山路', 'RD-ZSL-003', '次干路', 3.8, '南城区', '道路巡检二科', 'NORMAL'),
('解放路', 'RD-JFL-004', '主干路', 6.1, '北城区', '道路巡检二科', 'NORMAL'),
('长安街', 'RD-CAJ-005', '主干路', 10.2, '东城区', '道路巡检一科', 'NORMAL'),
('朝阳路', 'RD-CYL-006', '次干路', 4.5, '朝阳区', '道路巡检一科', 'NORMAL'),
('学院路', 'RD-XYL-007', '次干路', 3.2, '海淀区', '道路巡检二科', 'NORMAL'),
('中关村大街', 'RD-ZGC-008', '主干路', 7.8, '海淀区', '道路巡检二科', 'NORMAL')
ON DUPLICATE KEY UPDATE road_name=VALUES(road_name);
