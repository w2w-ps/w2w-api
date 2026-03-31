-- Create sequence for transaction_id
CREATE SEQUENCE IF NOT EXISTS scheduled_employee_transaction_id_seq START WITH 100000;

-- Set the column default to use the sequence
ALTER TABLE scheduled_employee ALTER COLUMN transaction_id SET DEFAULT nextval('scheduled_employee_transaction_id_seq');

-- Create sequence for schedule_id
CREATE SEQUENCE IF NOT EXISTS schedule_id_seq START WITH 100000;

-- Set the column default to use the sequence
ALTER TABLE schedule ALTER COLUMN schedule_id SET DEFAULT nextval('schedule_id_seq');
