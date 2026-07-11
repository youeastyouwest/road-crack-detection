CREATE TABLE IF NOT EXISTS detection_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    road_id BIGINT NULL,
    road_segment_id BIGINT NULL,
    location VARCHAR(255) NOT NULL,
    submitted_by VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    failure_reason VARCHAR(500) NULL,
    remark VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS detection_media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT NULL,
    frame_no INT NULL,
    captured_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_detection_media_task FOREIGN KEY (task_id) REFERENCES detection_task(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS detection_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL UNIQUE,
    summary VARCHAR(500) NULL,
    total_damage_count INT NOT NULL,
    highest_severity VARCHAR(20) NULL,
    avg_confidence DECIMAL(5,4) NULL,
    generated_work_order_id BIGINT NULL,
    completed_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_detection_result_task FOREIGN KEY (task_id) REFERENCES detection_task(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS detection_result_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    result_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    media_id BIGINT NULL,
    road_id BIGINT NULL,
    road_segment_id BIGINT NULL,
    damage_type VARCHAR(30) NOT NULL,
    severity_level VARCHAR(20) NOT NULL,
    confidence DECIMAL(5,4) NOT NULL,
    bbox_x INT NULL, bbox_y INT NULL, bbox_width INT NULL, bbox_height INT NULL,
    lng DECIMAL(12,7) NULL, lat DECIMAL(12,7) NULL,
    length_mm DECIMAL(10,2) NULL, width_mm DECIMAL(10,2) NULL, area_mm2 DECIMAL(14,2) NULL,
    snapshot_url VARCHAR(500) NULL,
    suggestion VARCHAR(500) NULL,
    detected_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_detection_result_item_result FOREIGN KEY (result_id) REFERENCES detection_result(id) ON DELETE CASCADE,
    CONSTRAINT fk_detection_result_item_task FOREIGN KEY (task_id) REFERENCES detection_task(id) ON DELETE CASCADE
);