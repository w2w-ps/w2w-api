-- Add accessibility_mode to employee table
ALTER TABLE employee ADD COLUMN IF NOT EXISTS accessibility_mode BOOLEAN DEFAULT FALSE;

-- Add partial unique index on (company_id, email) where email is not null
CREATE UNIQUE INDEX IF NOT EXISTS idx_employee_company_email
  ON employee (company_id, email)
  WHERE email IS NOT NULL AND status != 'Deleted';
