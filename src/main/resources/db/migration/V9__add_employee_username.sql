-- Add username column to employee table
ALTER TABLE employee ADD COLUMN username VARCHAR(255);

-- Populate username field for existing employees
UPDATE employee SET username = LOWER(first_name || last_name);
