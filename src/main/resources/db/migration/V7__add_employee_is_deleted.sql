-- Add is_deleted to employee table
ALTER TABLE employee ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Migrate existing "Deleted" status to is_deleted
UPDATE employee SET is_deleted = TRUE WHERE status = 'Deleted';
