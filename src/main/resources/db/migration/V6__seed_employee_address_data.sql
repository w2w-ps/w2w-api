-- V6: Seed employee addresses with more realistic data
-- This script ensures all employees have an address record and populates extra fields.

DO $$
DECLARE
    emp_record RECORD;
    cities TEXT[] := ARRAY['San Francisco', 'New York', 'Los Angeles', 'Chicago', 'Houston', 'Phoenix', 'Philadelphia', 'San Antonio', 'San Diego', 'Dallas'];
    states TEXT[] := ARRAY['CA', 'NY', 'CA', 'IL', 'TX', 'AZ', 'PA', 'TX', 'CA', 'TX'];
    random_idx INT;
BEGIN
    -- 1. Bypass RLS for the seeding process
    PERFORM set_config('app.internal_system_lookup', 'true', true);

    FOR emp_record IN SELECT employee_id FROM employee LOOP
        random_idx := floor(random() * array_length(cities, 1)) + 1;

        -- 2. Insert into employee_address
        INSERT INTO employee_address (employee_id, address, address2, city, state, zip)
        VALUES (
            emp_record.employee_id, 
            (floor(random()*9000)+100)::text || ' ' || (ARRAY['Oak', 'Maple', 'Cedar', 'Pine', 'Elm', 'Washington', 'Lincoln', 'Main', 'Broadway', 'Market'])[floor(random()*10)+1] || ' St', 
            CASE WHEN random() < 0.3 THEN 'Suite ' || (floor(random()*500)+1)::text ELSE NULL END, 
            cities[random_idx], 
            states[random_idx], 
            LPAD(floor(random()*90000+10000)::text, 5, '0')
        ) ON CONFLICT (employee_id) DO UPDATE SET
            address = EXCLUDED.address,
            address2 = EXCLUDED.address2,
            city = EXCLUDED.city,
            state = EXCLUDED.state,
            zip = EXCLUDED.zip;

        -- 3. Ensure other employee fields are populated if they are null
        UPDATE employee 
        SET 
            emp_type_id = COALESCE(emp_type_id, (floor(random() * 3) + 1)::INT),
            max_weekly_days = COALESCE(max_weekly_days, floor(random() * 5 + 1)::INT),
            max_daily_shifts = COALESCE(max_daily_shifts, floor(random() * 2 + 1)::INT),
            google_cal_export = COALESCE(google_cal_export, (random() < 0.2)),
            priority_group = COALESCE(priority_group, 'Group ' || CHR((65 + floor(random() * 3))::INT)),
            pay_rate = COALESCE(pay_rate, (floor(random() * 50) + 15)::FLOAT)
        WHERE employee_id = emp_record.employee_id;
    END LOOP;

    -- 4. Reset internal lookup
    PERFORM set_config('app.internal_system_lookup', 'false', true);
END $$;
