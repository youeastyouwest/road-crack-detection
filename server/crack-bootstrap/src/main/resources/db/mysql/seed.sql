-- ========================================
-- Auth & User Seed Data (All 6 Roles)
-- ========================================
-- Passwords below are BCrypt-encoded "123456"
-- Use the admin account to create more users via UI

INSERT IGNORE INTO role (id, name, code, description, status)
VALUES
(1, '超级管理员', 'ROLE_ADMIN',        '系统超级管理员', 1),
(2, '道路管理员',   'ROLE_ROAD_ADMIN',  '道路管理部门',   1),
(3, '环卫管理员',   'ROLE_SANIT_ADMIN', '环卫管理部门',   1),
(4, '交管管理员',   'ROLE_TRAFFIC_ADMIN','交通管理部门',  1),
(5, '维修工',       'ROLE_MAINTAINER',  '一线维修人员',   1),
(6, '众包人员',     'ROLE_CROWDSOURCE', '众包上报人员',   1);

-- Departments
INSERT IGNORE INTO department (id, name, code, parent_id, description, status)
VALUES
(1, '总管理处',     'HQ',         0, '总部', 1),
(2, '道路管理局',   'ROAD_ADMIN', 1, '道路管理部门', 1),
(3, '环卫管理局',   'SANITATION', 1, '环卫管理部门', 1),
(4, '交通管理局',   'TRAFFIC',    1, '交通管理部门', 1),
(5, '维修大队',     'MAINTEAM',   1, '维修队伍', 1);

-- Users: one for each role
-- BCrypt hash for "123456": $2a$10$DPD.uTyKWogW3ZTLojbtX.R2fEzvhSFLwY4zXIoU0YOXgiOJshvFy
INSERT IGNORE INTO `user` (id, username, password, real_name, phone, email, dept_id, status)
VALUES
(1, 'admin',        '$2a$10$DPD.uTyKWogW3ZTLojbtX.R2fEzvhSFLwY4zXIoU0YOXgiOJshvFy', '超级管理员', '13800001001', 'admin@roadcrack.cn', 1, 1),
(2, 'road_admin',   '$2a$10$DPD.uTyKWogW3ZTLojbtX.R2fEzvhSFLwY4zXIoU0YOXgiOJshvFy', '道路管理员', '13800001002', 'road@roadcrack.cn', 2, 1),
(3, 'sanit_admin',  '$2a$10$DPD.uTyKWogW3ZTLojbtX.R2fEzvhSFLwY4zXIoU0YOXgiOJshvFy', '环卫管理员', '13800001003', 'sanit@roadcrack.cn', 3, 1),
(4, 'traffic_admin','$2a$10$DPD.uTyKWogW3ZTLojbtX.R2fEzvhSFLwY4zXIoU0YOXgiOJshvFy', '交管管理员', '13800001004', 'traffic@roadcrack.cn', 4, 1),
(5, 'maintainer',   '$2a$10$DPD.uTyKWogW3ZTLojbtX.R2fEzvhSFLwY4zXIoU0YOXgiOJshvFy', '维修工小王', '13800001005', 'maintain@roadcrack.cn', 5, 1),
(6, 'crowd_user',   '$2a$10$DPD.uTyKWogW3ZTLojbtX.R2fEzvhSFLwY4zXIoU0YOXgiOJshvFy', '众包上报员', '13800001006', 'crowd@roadcrack.cn', 1, 1);

-- User-Role mapping
INSERT IGNORE INTO user_role (user_id, role_id) VALUES
(1, 1),  -- admin -> ROLE_ADMIN
(2, 2),  -- road_admin -> ROLE_ROAD_ADMIN
(3, 3),  -- sanit_admin -> ROLE_SANIT_ADMIN
(4, 4),  -- traffic_admin -> ROLE_TRAFFIC_ADMIN
(5, 5),  -- maintainer -> ROLE_MAINTAINER
(6, 6);  -- crowd_user -> ROLE_CROWDSOURCE

-- Seed 6 Demo Roads
INSERT IGNORE INTO road (id, road_code, road_name, road_grade, district, start_point, end_point, length_km, lane_count, surface_type, status, created_at)
VALUES
(1, 'RD-001', '长安街', '主干道', '东城区', '东单', '建国门', 3.5, 8, '沥青', 'ACTIVE', NOW()),
(2, 'RD-002', '东二环路', '快速路', '东城区', '东直门', '广渠门', 5.2, 6, '沥青', 'ACTIVE', NOW()),
(3, 'RD-003', '东三环路', '快速路', '朝阳区', '三元桥', '国贸桥', 6.8, 6, '沥青', 'ACTIVE', NOW()),
(4, 'RD-004', '朝阳路', '主干道', '朝阳区', '朝阳门', '管庄', 8.0, 4, '沥青', 'ACTIVE', NOW()),
(5, 'RD-005', '建国路', '主干道', '朝阳区', '建国门', '大望路', 4.5, 6, '沥青', 'ACTIVE', NOW()),
(6, 'RD-006', '通惠河北路', '次干道', '朝阳区', '东便门', '四惠', 5.0, 4, '沥青', 'ACTIVE', NOW());
