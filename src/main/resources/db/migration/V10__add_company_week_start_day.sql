-- Add week_start_day column to company table
-- 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
ALTER TABLE company ADD COLUMN week_start_day INTEGER DEFAULT 1;
