-- Performance optimization for scheduling queries
-- Composite indexes for scheduled_employee to handle multi-tenancy and common access patterns efficiently
CREATE INDEX idx_se_company_schedule ON scheduled_employee(company_id, schedule_id);
CREATE INDEX idx_se_company_employee ON scheduled_employee(company_id, employee_id);

-- Index for employee company filtering
CREATE INDEX idx_employee_company_id ON employee(company_id);

-- Metadata indexes to prevent sequential scans for larger databases
CREATE INDEX idx_skill_company_id ON skill(company_id);
CREATE INDEX idx_category_company_id ON category(company_id);
