-- Seed Data
DO $$
DECLARE
    first_names TEXT[] := ARRAY['James', 'Mary', 'Robert', 'Patricia', 'John', 'Jennifer', 'Michael', 'Linda', 'David', 'Elizabeth', 'William', 'Barbara', 'Richard', 'Susan', 'Joseph', 'Jessica', 'Thomas', 'Sarah', 'Charles', 'Karen', 'Christopher', 'Nancy', 'Daniel', 'Margaret', 'Matthew', 'Lisa', 'Anthony', 'Betty', 'Mark', 'Dorothy', 'Donald', 'Sandra', 'Steven', 'Ashley', 'Paul', 'Kimberly', 'Andrew', 'Donna', 'Joshua', 'Emily', 'Kenneth', 'Michelle', 'Kevin', 'Carol', 'Brian', 'Amanda', 'George', 'Melissa', 'Edward', 'Deborah'];
    last_names TEXT[] := ARRAY['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis', 'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez', 'Wilson', 'Anderson', 'Thomas', 'Taylor', 'Moore', 'Jackson', 'Martin', 'Lee', 'Perez', 'Thompson', 'White', 'Harris', 'Sanchez', 'Clark', 'Ramirez', 'Lewis', 'Robinson', 'Walker', 'Young', 'Allen', 'King', 'Wright', 'Scott', 'Torres', 'Nguyen', 'Hill', 'Flores', 'Green', 'Adams', 'Nelson', 'Baker', 'Hall', 'Rivera', 'Campbell', 'Mitchell', 'Carter', 'Roberts'];
    companies JSONB[] := ARRAY[
        '{"name": "Grand Plaza Hotel", "industry": "Hotel", "count": 50}'::JSONB,
        '{"name": "City General Hospital", "industry": "Hospital", "count": 150}'::JSONB,
        '{"name": "North Hub Distribution", "industry": "Warehouse", "count": 350}'::JSONB,
        '{"name": "Fresh Mart Supermarket", "industry": "Supermarket", "count": 1500}'::JSONB,
        '{"name": "Urban Threads Store", "industry": "Clothing Store", "count": 2500}'::JSONB,
        '{"name": "Global Logistics Center", "industry": "Logistics Center", "count": 10000}'::JSONB
    ];
    hotel_skills TEXT[] := ARRAY['Front Desk', 'Housekeeping', 'Bellhop', 'Chef', 'Concierge', 'Valet', 'Lifeguard', 'Event Coordinator', 'Room Service', 'Security', 'Maintenance', 'Mixologist', 'Server', 'Host', 'Pastry Chef'];
    hospital_skills TEXT[] := ARRAY['Registered Nurse', 'Radiologist', 'Surgeon', 'Anesthesiologist', 'Pharmacist', 'Physical Therapist', 'Lab Technician', 'ER Specialist', 'Pediatrician', 'Receptionist', 'Janitorial', 'Dietitian', 'Phlebotomist', 'Social Worker', 'Orderly'];
    warehouse_skills TEXT[] := ARRAY['Forklift Operator', 'Inventory Manager', 'Picker/Packer', 'Quality Control', 'Shipping Lead', 'Receiver', 'Dock Worker', 'Safety Officer', 'Data Entry', 'Loader', 'Maintenance', 'Team Lead', 'Order Clerk', 'Cycle Counter', 'Scanner Operator'];
    supermarket_skills TEXT[] := ARRAY['Cashier', 'Stock Clerk', 'Butcher', 'Baker', 'Produce Clerk', 'Deli Assistant', 'Customer Service', 'Bagger', 'Dairy Specialist', 'Frozen Food lead', 'Cart Attendant', 'Pharmacy Aide', 'Wine Steward', 'Florist', 'Receiver'];
    store_skills TEXT[] := ARRAY['Sales Associate', 'Visual Merchandiser', 'Cashier', 'Store Manager', 'Inventory Specialist', 'Loss Prevention', 'Personal Stylist', 'Tailor', 'Fulfillment lead', 'Janitorial', 'Stock Associate', 'Customer Support', 'Floor Lead', 'Key Holder', 'Display Coordinator'];
    logistics_skills TEXT[] := ARRAY['Dispatcher', 'Route Planner', 'Fleet Manager', 'Truck Driver', 'Operations Lead', 'Supply Chain Analyst', 'Warehouse Coordinator', 'Customs Specialist', 'Broker', 'Inventory Controller', 'Dock Manager', 'Safety Manager', 'Maintenance Tech', 'Compliance Officer', 'Project Manager'];
    category_names TEXT[] := ARRAY['Morning Shift', 'Afternoon Shift', 'Night Shift', 'Weekend Shift', 'Emergency', 'Holiday Shift', 'On-Call', 'Split Shift', 'Graveyard', 'Training'];

    comp JSONB;
    c_id INT := 1;
    e_id INT := 1;
    s_id INT := 1;
    cat_id INT := 1;
    sched_id INT := 1;
    trans_id INT := 1;
    
    target_count INT;
    industry_skills TEXT[];
    skill_desc TEXT;
    skill_count INT;
    f_name TEXT;
    l_name TEXT;
    i INT;
    j INT;
    m INT;
    v_current_date DATE;
    v_end_date DATE := '2026-04-30'::DATE;
    
    emp_ids INT[];
    comp_skill_ids INT[];
    comp_cat_ids INT[];
    
    is_overnight BOOLEAN;
    s_time TIME;
    e_time TIME;
    current_duration FLOAT;
BEGIN
    FOR i IN 1..array_length(companies, 1) LOOP
        comp := companies[i];
        target_count := (comp->>'count')::INT;
        emp_ids := ARRAY[]::INT[];
        comp_skill_ids := ARRAY[]::INT[];
        comp_cat_ids := ARRAY[]::INT[];
        INSERT INTO company (company_id, company_name, department_name, status)
        VALUES (c_id, comp->>'name', comp->>'industry', 'active');
        
        CASE comp->>'industry'
            WHEN 'Hotel' THEN industry_skills := hotel_skills;
            WHEN 'Hospital' THEN industry_skills := hospital_skills;
            WHEN 'Warehouse' THEN industry_skills := warehouse_skills;
            WHEN 'Supermarket' THEN industry_skills := supermarket_skills;
            WHEN 'Clothing Store' THEN industry_skills := store_skills;
            ELSE industry_skills := logistics_skills;
        END CASE;
        
        skill_count := 15 + floor(random() * 11)::INT;
        FOR j IN 1..skill_count LOOP
            skill_desc := industry_skills[((j-1) % array_length(industry_skills, 1)) + 1];
            INSERT INTO skill (skill_id, company_id, description, status, timestamp)
            VALUES (s_id, c_id, skill_desc, 'active', NOW());
            comp_skill_ids := array_append(comp_skill_ids, s_id);
            s_id := s_id + 1;
        END LOOP;
        
        FOR j IN 1..(5 + floor(random() * 6)::INT) LOOP
            INSERT INTO category (category_id, company_id, description, start_time, end_time)
            VALUES (cat_id, c_id, category_names[((j-1) % 10) + 1], '08:00', '17:00');
            comp_cat_ids := array_append(comp_cat_ids, cat_id);
            cat_id := cat_id + 1;
        END LOOP;
        
        FOR j IN 1..target_count LOOP
            f_name := first_names[floor(random() * array_length(first_names, 1)) + 1];
            l_name := last_names[floor(random() * array_length(last_names, 1)) + 1];
            INSERT INTO employee (employee_id, company_id, first_name, last_name, email, status, hire_date)
            VALUES (e_id, c_id, f_name, l_name, LOWER(f_name) || '.' || LOWER(l_name) || e_id || '@example.com', 'active', NOW());
            
            INSERT INTO employee_phone (employee_id, sort_order, phone_number) VALUES (e_id, 0, '555-' || LPAD(floor(random()*10000)::text, 4, '0'));
            INSERT INTO employee_phone (employee_id, sort_order, phone_number) VALUES (e_id, 1, '555-' || LPAD(floor(random()*10000)::text, 4, '0'));
            
            emp_ids := array_append(emp_ids, e_id);
            e_id := e_id + 1;
        END LOOP;

        -- Seed Skill Groups
        INSERT INTO skill_group (group_id, company_id, description) VALUES ((c_id * 10) + 1, c_id, 'Core Team');
        INSERT INTO skill_group (group_id, company_id, description) VALUES ((c_id * 10) + 2, c_id, 'Support Staff');
        
        INSERT INTO group_skill (group_id, skill_id)
        SELECT (c_id * 10) + 1, s.skill_id FROM skill s WHERE s.company_id = c_id AND random() < 0.3;
        
        -- Seed Category Groups
        INSERT INTO cat_group (group_id, company_id, description) VALUES ((c_id * 10) + 1, c_id, 'Standard Shifts');
        INSERT INTO group_cat (group_id, cat_id)
        SELECT (c_id * 10) + 1, cat.category_id FROM category cat WHERE cat.company_id = c_id AND random() < 0.5;

        -- Seed Employee Skills
        INSERT INTO employee_skill (employee_id, skill_id)
        SELECT e.employee_id, s.skill_id
        FROM employee e
        JOIN skill s ON e.company_id = s.company_id
        WHERE e.company_id = c_id AND random() < 0.2;

        -- Ensure every employee has at least one skill
        INSERT INTO employee_skill (employee_id, skill_id)
        SELECT e.employee_id, (SELECT s.skill_id FROM skill s WHERE s.company_id = c_id LIMIT 1)
        FROM employee e
        WHERE e.company_id = c_id AND NOT EXISTS (SELECT 1 FROM employee_skill es WHERE es.employee_id = e.employee_id);

        v_current_date := '2026-03-01'::DATE;
        WHILE v_current_date <= v_end_date LOOP
            INSERT INTO schedule (schedule_id, company_id, description, start_date, day_of_week, is_published, timestamp)
            VALUES (sched_id, c_id, 'Daily Schedule: ' || v_current_date, v_current_date, (extract(dow from v_current_date))::SMALLINT, TRUE, NOW());
            FOR m IN 1..(CASE WHEN target_count > 300 THEN 300 ELSE target_count END) LOOP
                IF random() < 0.3 THEN
                    is_overnight := (random() < 0.2);
                    IF is_overnight THEN
                        s_time := '22:00:00'::TIME;
                        e_time := '06:00:00'::TIME;
                        current_duration := 8.0;
                    ELSE
                        s_time := '08:00:00'::TIME;
                        e_time := '16:00:00'::TIME;
                        current_duration := 8.0;
                    END IF;

                    INSERT INTO scheduled_employee (
                        shift_id, employee_id, schedule_id, company_id,
                        description, start_time, end_time, is_overnight, duration,
                        required_skill_id, category_id
                    )
                    VALUES (
                               trans_id, emp_ids[m], sched_id, c_id,
                               'Generated Shift ' || trans_id, s_time, e_time, is_overnight, current_duration,
                               comp_skill_ids[floor(random() * array_length(comp_skill_ids, 1)) + 1],
                               comp_cat_ids[floor(random() * array_length(comp_cat_ids, 1)) + 1]
                           );
                    trans_id := trans_id + 1;
                END IF;
            END LOOP;
            v_current_date := v_current_date + 1;
            sched_id := sched_id + 1;
        END LOOP;
        c_id := c_id + 1;
    END LOOP;
END $$;
