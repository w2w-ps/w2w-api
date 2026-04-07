CREATE SEQUENCE IF NOT EXISTS employee_id_seq START WITH 100000;
ALTER TABLE employee ALTER COLUMN employee_id SET DEFAULT nextval('employee_id_seq');
-- Update existing sequence to be above current max ID
SELECT setval('employee_id_seq', (SELECT COALESCE(MAX(employee_id), 0) + 1 FROM employee));
