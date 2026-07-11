CREATE TABLE IF NOT EXISTS maintenance_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_code VARCHAR(50) NOT NULL UNIQUE,
    work_order_id BIGINT NOT NULL,
    executor VARCHAR(100) NOT NULL,
    before_image_url VARCHAR(500) NULL,
    after_image_url VARCHAR(500) NULL,
    materials VARCHAR(255) NULL,
    description TEXT NULL,
    finished_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_maintenance_report_work_order FOREIGN KEY (work_order_id) REFERENCES work_order(id) ON DELETE CASCADE
);