-- Add is_deleted column to scheduled_employee table for soft-delete support
ALTER TABLE scheduled_employee ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
