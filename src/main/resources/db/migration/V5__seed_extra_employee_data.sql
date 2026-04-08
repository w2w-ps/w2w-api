-- Seed addresses and extra fields for existing employees

DO $$
DECLARE
    emp_record RECORD;
BEGIN
    FOR emp_record IN SELECT employee_id FROM employee LOOP
        -- 1. Insert into employee_address
        -- Only insert if it doesn't already exist (though this is a new table, it's safer)
        INSERT INTO employee_address (employee_id, address, address2, city, state, zip)
        VALUES (
            emp_record.employee_id, 
            (floor(random()*9000)+1000)::text || ' Main St', 
            'Apt ' || floor(random()*100)::text, 
            'Springfield', 
            'IL', 
            LPAD(floor(random()*90000+10000)::text, 5, '0')
        ) ON CONFLICT (employee_id) DO NOTHING;

        -- 2. Update existing employee fields
        UPDATE employee 
        SET 
            emp_type_id = (floor(random() * 3) + 1)::INT,
            max_weekly_days = floor(random() * 5 + 1),
            max_daily_shifts = floor(random() * 2 + 1),
            google_cal_export = (random() < 0.2),
            priority_group = 'Group ' || CHR((65 + floor(random() * 3))::INT)
        WHERE employee_id = emp_record.employee_id;
    END LOOP;
END $$;
