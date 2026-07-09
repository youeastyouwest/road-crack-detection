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

CREATE TABLE IF NOT EXISTS user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_role_user_id (user_id),
    KEY idx_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL,
    username VARCHAR(64) DEFAULT '',
    module VARCHAR(64) DEFAULT '',
    action VARCHAR(64) DEFAULT '',
    description VARCHAR(500) DEFAULT '',
    ip VARCHAR(64) DEFAULT '',
    params TEXT,
    status TINYINT DEFAULT 1,
    error_msg VARCHAR(500) DEFAULT '',
    cost_time BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_operation_log_user_id (user_id),
    KEY idx_operation_log_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO department (name, code, parent_id, description, sort_order)
SELECT 'System', 'DEPT_SYS', 0, 'System management', 1
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'DEPT_SYS');

INSERT INTO department (name, code, parent_id, description, sort_order)
SELECT 'Road', 'DEPT_ROAD', 0, 'Road maintenance', 2
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'DEPT_ROAD');

INSERT INTO department (name, code, parent_id, description, sort_order)
SELECT 'Sanitation', 'DEPT_SANIT', 0, 'Road sanitation', 3
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'DEPT_SANIT');

INSERT INTO department (name, code, parent_id, description, sort_order)
SELECT 'Traffic', 'DEPT_TRAFFIC', 0, 'Traffic coordination', 4
WHERE NOT EXISTS (SELECT 1 FROM department WHERE code = 'DEPT_TRAFFIC');

INSERT INTO role (name, code, description)
SELECT 'Admin', 'ROLE_ADMIN', 'System administrator'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE code = 'ROLE_ADMIN');

INSERT INTO role (name, code, description)
SELECT 'Dispatcher', 'ROLE_DISPATCHER', 'Work order dispatcher'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE code = 'ROLE_DISPATCHER');

INSERT INTO role (name, code, description)
SELECT 'Maintainer', 'ROLE_MAINTAINER', 'Maintenance operator'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE code = 'ROLE_MAINTAINER');

INSERT INTO role (name, code, description)
SELECT 'Crowdsource', 'ROLE_CROWDSOURCE', 'Crowdsource submitter'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE code = 'ROLE_CROWDSOURCE');

INSERT INTO `user` (username, password, real_name, dept_id, status)
SELECT 'admin', '$2a$10$h7A2nNFXpuYzl0HRQZBxceITpVH40SiSRk2sQVDGbBRyYglG7Qq/W', 'Admin', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'admin');

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM `user` u
JOIN role r ON r.code = 'ROLE_ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO user_role (user_id, role_id)
SELECT DISTINCT ur.user_id, dispatcher_role.id
FROM user_role ur
JOIN role legacy_role ON legacy_role.id = ur.role_id
JOIN role dispatcher_role ON dispatcher_role.code = 'ROLE_DISPATCHER'
WHERE legacy_role.code IN (
      'ROLE_INSPECTOR',
      'ROLE_REVIEWER',
      'ROLE_ROAD_ADMIN',
      'ROLE_SANIT_ADMIN',
      'ROLE_TRAFFIC_ADMIN'
  )
  AND NOT EXISTS (
      SELECT 1 FROM user_role existing
      WHERE existing.user_id = ur.user_id
        AND existing.role_id = dispatcher_role.id
  );

DELETE ur
FROM user_role ur
JOIN role legacy_role ON legacy_role.id = ur.role_id
WHERE legacy_role.code IN (
      'ROLE_INSPECTOR',
      'ROLE_REVIEWER',
      'ROLE_ROAD_ADMIN',
      'ROLE_SANIT_ADMIN',
      'ROLE_TRAFFIC_ADMIN'
  );

DELETE FROM role
WHERE code IN (
      'ROLE_INSPECTOR',
      'ROLE_REVIEWER',
      'ROLE_ROAD_ADMIN',
      'ROLE_SANIT_ADMIN',
      'ROLE_TRAFFIC_ADMIN'
  );
