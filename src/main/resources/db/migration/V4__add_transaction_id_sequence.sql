-- Create sequence for transaction_id
CREATE SEQUENCE IF NOT EXISTS scheduled_employee_transaction_id_seq START WITH 100000;

-- Set the column default to use the sequence
ALTER TABLE scheduled_employee ALTER COLUMN transaction_id SET DEFAULT nextval('scheduled_employee_transaction_id_seq');

-- Create sequence for schedule_id
CREATE SEQUENCE IF NOT EXISTS schedule_id_seq START WITH 100000;

-- Set the column default to use the sequence
ALTER TABLE schedule ALTER COLUMN schedule_id SET DEFAULT nextval('schedule_id_seq');

-- Add soft-delete support for shifts
ALTER TABLE scheduled_employee ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;

-- Store shift color as a string value
ALTER TABLE scheduled_employee
ALTER COLUMN color TYPE VARCHAR(255)
USING color::VARCHAR(255);

-- Convert the schedule publish flag to a native boolean
ALTER TABLE schedule
ALTER COLUMN published TYPE BOOLEAN
USING CASE
    WHEN published IS NULL THEN NULL
    WHEN LOWER(published) = 'true' THEN TRUE
    WHEN LOWER(published) = 'false' THEN FALSE
    ELSE NULL
END;

-- Rename the publish flag to match the boolean naming convention
ALTER TABLE schedule
RENAME COLUMN published TO is_published;
