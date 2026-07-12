-- Migration: add review-related columns to maintenance_report for existing databases
ALTER TABLE maintenance_report
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'PENDING' AFTER finished_at,
    ADD COLUMN IF NOT EXISTS reviewer VARCHAR(100) NULL AFTER status,
    ADD COLUMN IF NOT EXISTS review_remark VARCHAR(500) NULL AFTER reviewer,
    ADD COLUMN IF NOT EXISTS reviewed_at DATETIME NULL AFTER review_remark;
