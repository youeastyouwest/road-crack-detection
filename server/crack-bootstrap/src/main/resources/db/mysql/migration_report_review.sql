-- Migration: Add review workflow columns to maintenance_report table
-- This enables the dept-admin → admin review flow

ALTER TABLE maintenance_report ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
ALTER TABLE maintenance_report ADD COLUMN review_remark VARCHAR(500) NULL;
ALTER TABLE maintenance_report ADD COLUMN reviewer VARCHAR(100) NULL;
ALTER TABLE maintenance_report ADD COLUMN reviewed_at DATETIME NULL;

-- Update existing reports to PENDING status (they were previously auto-closed, now need review)
-- If there are existing reports, their associated work orders are already CLOSED,
-- so we mark them as APPROVED to maintain backward compatibility
UPDATE maintenance_report SET status = 'APPROVED' WHERE status = 'PENDING'
  AND EXISTS (SELECT 1 FROM work_order WHERE work_order.id = maintenance_report.work_order_id AND work_order.status = 'CLOSED');
