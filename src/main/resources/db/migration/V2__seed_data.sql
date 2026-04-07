-- Seed Data (DML)
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
    hotel_positions TEXT[] := ARRAY['Front Desk', 'Housekeeping', 'Bellhop', 'Chef', 'Concierge', 'Valet', 'Lifeguard', 'Event Coordinator', 'Room Service', 'Security', 'Maintenance', 'Mixologist', 'Server', 'Host', 'Pastry Chef'];
    hospital_positions TEXT[] := ARRAY['Registered Nurse', 'Radiologist', 'Surgeon', 'Anesthesiologist', 'Pharmacist', 'Physical Therapist', 'Lab Technician', 'ER Specialist', 'Pediatrician', 'Receptionist', 'Janitorial', 'Dietitian', 'Phlebotomist', 'Social Worker', 'Orderly'];
    warehouse_positions TEXT[] := ARRAY['Forklift Operator', 'Inventory Manager', 'Picker/Packer', 'Quality Control', 'Shipping Lead', 'Receiver', 'Dock Worker', 'Safety Officer', 'Data Entry', 'Loader', 'Maintenance', 'Team Lead', 'Order Clerk', 'Cycle Counter', 'Scanner Operator'];
    supermarket_positions TEXT[] := ARRAY['Cashier', 'Stock Clerk', 'Butcher', 'Baker', 'Produce Clerk', 'Deli Assistant', 'Customer Service', 'Bagger', 'Dairy Specialist', 'Frozen Food lead', 'Cart Attendant', 'Pharmacy Aide', 'Wine Steward', 'Florist', 'Receiver'];
    store_positions TEXT[] := ARRAY['Sales Associate', 'Visual Merchandiser', 'Cashier', 'Store Manager', 'Inventory Specialist', 'Loss Prevention', 'Personal Stylist', 'Tailor', 'Fulfillment lead', 'Janitorial', 'Stock Associate', 'Customer Support', 'Floor Lead', 'Key Holder', 'Display Coordinator'];
    logistics_positions TEXT[] := ARRAY['Dispatcher', 'Route Planner', 'Fleet Manager', 'Truck Driver', 'Operations Lead', 'Supply Chain Analyst', 'Warehouse Coordinator', 'Customs Specialist', 'Broker', 'Inventory Controller', 'Dock Manager', 'Safety Manager', 'Maintenance Tech', 'Compliance Officer', 'Project Manager'];
    category_names TEXT[] := ARRAY['Morning Shift', 'Afternoon Shift', 'Night Shift', 'Weekend Shift', 'Emergency', 'Holiday Shift', 'On-Call', 'Split Shift', 'Graveyard', 'Training'];

    comp JSONB;
    c_id INT := 1;
    e_id INT := 1;
    p_id INT := 1;
    cat_id INT := 1;
    sched_id INT := 1;
    trans_id INT := 1;
    
    target_count INT;
    industry_positions TEXT[];
    pos_desc TEXT;
    pos_count INT;
    f_name TEXT;
    l_name TEXT;
    i INT;
    j INT;
    m INT;
    v_current_date DATE;
    v_end_date DATE := '2026-04-30'::DATE;
    
    emp_ids INT[];
    comp_pos_ids INT[];
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
        comp_pos_ids := ARRAY[]::INT[];
        comp_cat_ids := ARRAY[]::INT[];
        INSERT INTO company (company_id, company_name, department_name, status)
        VALUES (c_id, comp->>'name', comp->>'industry', 'active');
        
        CASE comp->>'industry'
            WHEN 'Hotel' THEN industry_positions := hotel_positions;
            WHEN 'Hospital' THEN industry_positions := hospital_positions;
            WHEN 'Warehouse' THEN industry_positions := warehouse_positions;
            WHEN 'Supermarket' THEN industry_positions := supermarket_positions;
            WHEN 'Clothing Store' THEN industry_positions := store_positions;
            ELSE industry_positions := logistics_positions;
        END CASE;
        
        pos_count := 15 + floor(random() * 11)::INT;
        FOR j IN 1..pos_count LOOP
            pos_desc := industry_positions[((j-1) % array_length(industry_positions, 1)) + 1];
            INSERT INTO position (position_id, company_id, description, is_deleted, timestamp)
            VALUES (p_id, c_id, pos_desc, FALSE, NOW());
            comp_pos_ids := array_append(comp_pos_ids, p_id);
            p_id := p_id + 1;
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

        -- Seed Position Groups
        INSERT INTO position_group (group_id, company_id, description) VALUES ((c_id * 10) + 1, c_id, 'Core Team');
        INSERT INTO position_group (group_id, company_id, description) VALUES ((c_id * 10) + 2, c_id, 'Support Staff');
        
        INSERT INTO group_position (group_id, position_id)
        SELECT (c_id * 10) + 1, p.position_id FROM position p WHERE p.company_id = c_id AND random() < 0.3;
        
        -- Seed Category Groups
        INSERT INTO cat_group (group_id, company_id, description) VALUES ((c_id * 10) + 1, c_id, 'Standard Shifts');
        INSERT INTO group_cat (group_id, cat_id)
        SELECT (c_id * 10) + 1, cat.category_id FROM category cat WHERE cat.company_id = c_id AND random() < 0.5;

        -- Seed Employee Positions
        INSERT INTO employee_position (employee_id, position_id)
        SELECT e.employee_id, p.position_id
        FROM employee e
        JOIN position p ON e.company_id = p.company_id
        WHERE e.company_id = c_id AND random() < 0.2;

        -- Ensure every employee has at least one position
        INSERT INTO employee_position (employee_id, position_id)
        SELECT e.employee_id, (SELECT p.position_id FROM position p WHERE p.company_id = c_id LIMIT 1)
        FROM employee e
        WHERE e.company_id = c_id AND NOT EXISTS (SELECT 1 FROM employee_position ep WHERE ep.employee_id = e.employee_id);

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
                        required_position_id, category_id
                    )
                    VALUES (
                               trans_id, emp_ids[m], sched_id, c_id,
                               'Generated Shift ' || trans_id, s_time, e_time, is_overnight, current_duration,
                               comp_pos_ids[floor(random() * array_length(comp_pos_ids, 1)) + 1],
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

-- Fix sequences after mass insert
SELECT setval('employee_id_seq', (SELECT MAX(employee_id) FROM employee));
SELECT setval('schedule_id_seq', (SELECT MAX(schedule_id) FROM schedule));
SELECT setval('scheduled_employee_shift_id_seq', (SELECT MAX(shift_id) FROM scheduled_employee));
SELECT setval('position_group_id_seq', (SELECT MAX(group_id) FROM position_group));

-- Seed security tables
INSERT INTO companies (company_name) VALUES ('Default Company');

INSERT INTO emp_type (emp_type_name) VALUES 
('Full Time'),
('Part Time'),
('Per Diem');

INSERT INTO user_roles (role_name) VALUES 
('Manager'),
('Employee'),
('AddManager');

INSERT INTO users (
    user_login_id,
    user_login_pw,
    company_id,
    emp_type_id,
    role_id,
    employee_id,
    encryption_type,
    login_failures
) VALUES (
    'admin',
    '$2a$12$9B69QSXuEqf6bgZcWbJXMOc0RHlFkwHQ4iInRtrIwiC9nAJSTgdk.',
    1,
    1,
    1,
    1,
    0,
    0
);

INSERT INTO manager_permissions (user_id, is_main_manager, can_manage_positions, can_manage_team_members, can_edit_shifts)
VALUES (1, TRUE, TRUE, TRUE, TRUE);

-- Seed preferences
INSERT INTO day_prefs (employee_id, date, prefs, compression, edited_by, is_day_prefs)
SELECT 1, '2026-04-01'::DATE + i, 'DDDDDDDDDDDDDDDDDDDDPPPPPPPPDDDPPPPPPDDDDPPPPPPPPPPPPPPCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCNNNNN', 0, 1, FALSE
FROM generate_series(0, 30) i;

INSERT INTO week_prefs (employee_id, start_date, prefs, compression, edited_by)
VALUES (1, '2026-04-06', 'DDDDDDDDDDDDDNDDDDDDPPPPPPPPDDDPPPPPPDDDDPPPPPPPPPPPPPPCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPNNNNNNPPPPPPPPPPPPPPPCCCCCCCCCCCCCCCCCCCCPPPPPPPPCPPPPPPPPPPPPPPPPPPPPPPPPCCCPPCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPCCCCCCCCCCCCCCCCCCCCPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPCCPCPPCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDDDDDPPPPPPPPDDDDDDDDDDDDDDDDDPPPPPPPPPPPPPPPPPPPDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP', 0, 1);
