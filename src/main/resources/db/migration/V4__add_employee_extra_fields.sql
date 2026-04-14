-- Add missing fields and configuration tables for Employee List

-- 1. Create employee_address table
CREATE TABLE IF NOT EXISTS employee_address (
    employee_id INTEGER PRIMARY KEY REFERENCES employee(employee_id) ON DELETE CASCADE,
    address VARCHAR(255),
    address2 VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    zip VARCHAR(20)
);

-- 2. Add extra fields to employee table
ALTER TABLE employee ADD COLUMN IF NOT EXISTS emp_type_id INTEGER REFERENCES emp_type(emp_type_id);
ALTER TABLE employee ADD COLUMN IF NOT EXISTS max_weekly_days INTEGER;
ALTER TABLE employee ADD COLUMN IF NOT EXISTS max_daily_shifts INTEGER;
ALTER TABLE employee ADD COLUMN IF NOT EXISTS comments TEXT;
ALTER TABLE employee ADD COLUMN IF NOT EXISTS priority_group VARCHAR(255);
ALTER TABLE employee ADD COLUMN IF NOT EXISTS google_cal_export BOOLEAN DEFAULT FALSE;
ALTER TABLE employee ADD COLUMN IF NOT EXISTS next_alert_date DATE;
ALTER TABLE employee ADD COLUMN IF NOT EXISTS custom_field_1 VARCHAR(255);
ALTER TABLE employee ADD COLUMN IF NOT EXISTS custom_field_2 VARCHAR(255);
ALTER TABLE employee ADD COLUMN IF NOT EXISTS employee_photo VARCHAR(255);

-- 3. Create employee_list_config table
CREATE TABLE IF NOT EXISTS employee_list_config (
    config_id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(company_id),
    column_name VARCHAR(255) NOT NULL,
    is_visible BOOLEAN DEFAULT TRUE,
    UNIQUE(company_id, column_name)
);

-- 4. Enable RLS and add policies
ALTER TABLE employee_address ENABLE ROW LEVEL SECURITY;
ALTER TABLE employee_address FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON employee_address
  USING (
    EXISTS (
      SELECT 1 FROM employee e
      WHERE e.employee_id = employee_address.employee_id
        AND e.company_id = current_setting('app.current_tenant', true)::INTEGER
    )
    OR current_setting('app.internal_system_lookup', true)::TEXT = 'true'
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM employee e
      WHERE e.employee_id = employee_address.employee_id
        AND e.company_id = current_setting('app.current_tenant', true)::INTEGER
    )
    OR current_setting('app.internal_system_lookup', true)::TEXT = 'true'
  );

ALTER TABLE employee_list_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE employee_list_config FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON employee_list_config
  USING (
    current_setting('app.current_tenant', true)::INTEGER = company_id
    OR current_setting('app.internal_system_lookup', true)::TEXT = 'true'
  )
  WITH CHECK (
    current_setting('app.current_tenant', true)::INTEGER = company_id
    OR current_setting('app.internal_system_lookup', true)::TEXT = 'true'
  );
