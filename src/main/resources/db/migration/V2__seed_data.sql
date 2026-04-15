-- Seed Data (DML)

-- ============================================================
-- SECTION 1: Tenant-scoped demo data (tenants 1–6)
-- app.current_tenant is set per company inside the loop so every insert
-- satisfies the RLS WITH CHECK policy for that company.
-- ============================================================
DO $$
DECLARE
    first_names TEXT[] := ARRAY['James', 'Mary', 'Robert', 'Patricia', 'John', 'Jennifer', 'Michael', 'Linda', 'David', 'Elizabeth', 'William', 'Barbara', 'Richard', 'Susan', 'Joseph', 'Jessica', 'Thomas', 'Sarah', 'Charles', 'Karen', 'Christopher', 'Nancy', 'Daniel', 'Margaret', 'Matthew', 'Lisa', 'Anthony', 'Betty', 'Mark', 'Dorothy', 'Donald', 'Sandra', 'Steven', 'Ashley', 'Paul', 'Kimberly', 'Andrew', 'Donna', 'Joshua', 'Emily', 'Kenneth', 'Michelle', 'Kevin', 'Carol', 'Brian', 'Amanda', 'George', 'Melissa', 'Edward', 'Deborah'];
    last_names TEXT[] := ARRAY['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis', 'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez', 'Wilson', 'Anderson', 'Thomas', 'Taylor', 'Moore', 'Jackson', 'Martin', 'Lee', 'Perez', 'Thompson', 'White', 'Harris', 'Sanchez', 'Clark', 'Ramirez', 'Lewis', 'Robinson', 'Walker', 'Young', 'Allen', 'King', 'Wright', 'Scott', 'Torres', 'Nguyen', 'Hill', 'Flores', 'Green', 'Adams', 'Nelson', 'Baker', 'Hall', 'Rivera', 'Campbell', 'Mitchell', 'Carter', 'Roberts'];
    company_seeds JSONB[] := ARRAY[
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
    category_short_descs TEXT[] := ARRAY['MORN', 'AFT', 'NGT', 'WKND', 'EMRG', 'HOL', 'CALL', 'SPLT', 'GRVY', 'TRNG'];

    company_seed JSONB;
    company_id_value INT := 1;
    employee_id_value INT := 1;
    position_id_value INT := 1;
    category_id_value INT := 1;
    schedule_id_value INT := 1;
    shift_id_value INT := 1;

    target_count INT;
    industry_positions TEXT[];
    pos_desc TEXT;
    pos_count INT;
    first_name_value TEXT;
    last_name_value TEXT;
    i INT;
    j INT;
    m INT;
    current_date_value DATE;
    end_date_value DATE := '2026-04-30'::DATE;

    employee_ids INT[];
    company_position_ids INT[];
    company_category_ids INT[];

    is_overnight BOOLEAN;
    start_time_value TIME;
    end_time_for_shift TIME;
    current_duration FLOAT;
BEGIN
    FOR i IN 1..array_length(company_seeds, 1) LOOP
        company_seed := company_seeds[i];
        target_count := (company_seed->>'count')::INT;
        employee_ids := ARRAY[]::INT[];
        company_position_ids := ARRAY[]::INT[];
        company_category_ids := ARRAY[]::INT[];

        -- Set tenant context so all inserts for this company satisfy RLS WITH CHECK.
        PERFORM set_config('app.current_tenant', company_id_value::TEXT, false);

        INSERT INTO company (company_id, company_name, department_name, status)
        VALUES (company_id_value, company_seed->>'name', company_seed->>'industry', 'active');

        CASE company_seed->>'industry'
            WHEN 'Hotel' THEN industry_positions := hotel_positions;
            WHEN 'Hospital' THEN industry_positions := hospital_positions;
            WHEN 'Warehouse' THEN industry_positions := warehouse_positions;
            WHEN 'Supermarket' THEN industry_positions := supermarket_positions;
            WHEN 'Clothing Store' THEN industry_positions := store_positions;
            ELSE industry_positions := logistics_positions;
        END CASE;

        pos_count := 15 + floor(random() * 11)::INT;
        FOR j IN 1..pos_count LOOP
            pos_desc := industry_positions[((j - 1) % array_length(industry_positions, 1)) + 1];
            INSERT INTO position (position_id, company_id, description, is_deleted, timestamp)
            VALUES (position_id_value, company_id_value, pos_desc, FALSE, NOW());
            company_position_ids := array_append(company_position_ids, position_id_value);
            position_id_value := position_id_value + 1;
        END LOOP;

        FOR j IN 1..(5 + floor(random() * 6)::INT) LOOP
            INSERT INTO category (category_id, company_id, short_desc, description, start_time, end_time)
            VALUES (
                category_id_value,
                company_id_value,
                category_short_descs[((j - 1) % array_length(category_short_descs, 1)) + 1],
                category_names[((j - 1) % array_length(category_names, 1)) + 1],
                '08:00',
                '17:00'
            );
            company_category_ids := array_append(company_category_ids, category_id_value);
            category_id_value := category_id_value + 1;
        END LOOP;

        FOR j IN 1..target_count LOOP
            first_name_value := first_names[floor(random() * array_length(first_names, 1)) + 1];
            last_name_value := last_names[floor(random() * array_length(last_names, 1)) + 1];
            INSERT INTO employee (employee_id, company_id, first_name, last_name, email, status, hire_date)
            VALUES (
                employee_id_value,
                company_id_value,
                first_name_value,
                last_name_value,
                LOWER(first_name_value) || '.' || LOWER(last_name_value) || employee_id_value || '@example.com',
                'active',
                NOW()
            );

            INSERT INTO employee_phone (employee_id, sort_order, phone_number)
            VALUES (employee_id_value, 0, '555-' || LPAD(floor(random() * 10000)::TEXT, 4, '0'));
            INSERT INTO employee_phone (employee_id, sort_order, phone_number)
            VALUES (employee_id_value, 1, '555-' || LPAD(floor(random() * 10000)::TEXT, 4, '0'));

            employee_ids := array_append(employee_ids, employee_id_value);
            employee_id_value := employee_id_value + 1;
        END LOOP;

        INSERT INTO position_group (group_id, company_id, description)
        VALUES ((company_id_value * 10) + 1, company_id_value, 'Core Team');
        INSERT INTO position_group (group_id, company_id, description)
        VALUES ((company_id_value * 10) + 2, company_id_value, 'Support Staff');

        INSERT INTO group_position (group_id, position_id)
        SELECT (company_id_value * 10) + 1, p.position_id
        FROM position p
        WHERE p.company_id = company_id_value
          AND random() < 0.3;

        INSERT INTO cat_group (group_id, company_id, description)
        VALUES ((company_id_value * 10) + 1, company_id_value, 'Standard Shifts');
        INSERT INTO group_cat (group_id, cat_id)
        SELECT (company_id_value * 10) + 1, cat.category_id
        FROM category cat
        WHERE cat.company_id = company_id_value
          AND random() < 0.5;

        INSERT INTO employee_position (employee_id, position_id)
        SELECT e.employee_id, p.position_id
        FROM employee e
        JOIN position p ON e.company_id = p.company_id
        WHERE e.company_id = company_id_value
          AND random() < 0.2;

        INSERT INTO employee_position (employee_id, position_id)
        SELECT e.employee_id, (SELECT p.position_id FROM position p WHERE p.company_id = company_id_value LIMIT 1)
        FROM employee e
        WHERE e.company_id = company_id_value
          AND NOT EXISTS (
              SELECT 1
              FROM employee_position ep
              WHERE ep.employee_id = e.employee_id
          );

        current_date_value := '2026-03-01'::DATE;
        WHILE current_date_value <= end_date_value LOOP
            INSERT INTO schedule (schedule_id, company_id, description, start_date, day_of_week, is_published, timestamp)
            VALUES (
                schedule_id_value,
                company_id_value,
                'Daily Schedule: ' || current_date_value,
                current_date_value,
                extract(dow FROM current_date_value)::SMALLINT,
                TRUE,
                NOW()
            );

            FOR m IN 1..(CASE WHEN target_count > 300 THEN 300 ELSE target_count END) LOOP
                IF random() < 0.3 THEN
                    is_overnight := (random() < 0.2);
                    IF is_overnight THEN
                        start_time_value := '22:00:00'::TIME;
                        end_time_for_shift := '06:00:00'::TIME;
                        current_duration := 8.0;
                    ELSE
                        start_time_value := '08:00:00'::TIME;
                        end_time_for_shift := '16:00:00'::TIME;
                        current_duration := 8.0;
                    END IF;

                    INSERT INTO scheduled_employee (
                        shift_id,
                        employee_id,
                        schedule_id,
                        company_id,
                        description,
                        start_time,
                        end_time,
                        is_overnight,
                        duration,
                        required_position_id,
                        category_id
                    )
                    VALUES (
                        shift_id_value,
                        employee_ids[m],
                        schedule_id_value,
                        company_id_value,
                        'Generated Shift ' || shift_id_value,
                        start_time_value,
                        end_time_for_shift,
                        is_overnight,
                        current_duration,
                        company_position_ids[floor(random() * array_length(company_position_ids, 1)) + 1],
                        company_category_ids[floor(random() * array_length(company_category_ids, 1)) + 1]
                    );
                    shift_id_value := shift_id_value + 1;
                END IF;
            END LOOP;

            current_date_value := current_date_value + 1;
            schedule_id_value := schedule_id_value + 1;
        END LOOP;

        company_id_value := company_id_value + 1;
    END LOOP;
END $$;

SELECT setval('employee_id_seq', (SELECT MAX(employee_id) FROM employee));
SELECT setval('schedule_id_seq', (SELECT MAX(schedule_id) FROM schedule));
SELECT setval('scheduled_employee_shift_id_seq', (SELECT MAX(shift_id) FROM scheduled_employee));
SELECT setval('position_group_id_seq', (SELECT MAX(group_id) FROM position_group));
SELECT setval('position_position_id_seq', (SELECT MAX(position_id) FROM position));
SELECT setval('category_id_seq', (SELECT MAX(category_id) FROM category));
SELECT setval('category_group_id_seq', (SELECT MAX(group_id) FROM cat_group));

INSERT INTO emp_type (emp_type_name)
VALUES ('Full Time'), ('Part Time'), ('Per Diem');

INSERT INTO user_roles (role_name)
VALUES ('Manager'), ('Employee'), ('AddManager');

INSERT INTO users (
    user_login_id,
    user_login_pw,
    company_id,
    emp_type_id,
    role_id,
    employee_id,
    encryption_type,
    login_failures
)
VALUES (
    'admin',
    '$2a$12$9B69QSXuEqf6bgZcWbJXMOc0RHlFkwHQ4iInRtrIwiC9nAJSTgdk.',
    1,
    1,
    1,
    1,
    0,
    0
);

DO $$
DECLARE
    default_password_hash CONSTANT TEXT := '$2a$12$9B69QSXuEqf6bgZcWbJXMOc0RHlFkwHQ4iInRtrIwiC9nAJSTgdk.';
    full_time_id INTEGER;
    employee_role_id INTEGER;
    manager_role_id INTEGER;
    add_manager_role_id INTEGER;
BEGIN
    SELECT emp_type_id INTO full_time_id
    FROM emp_type
    WHERE emp_type_name = 'Full Time';

    SELECT role_id INTO employee_role_id
    FROM user_roles
    WHERE role_name = 'Employee';

    SELECT role_id INTO manager_role_id
    FROM user_roles
    WHERE role_name = 'Manager';

    SELECT role_id INTO add_manager_role_id
    FROM user_roles
    WHERE role_name = 'AddManager';

    IF full_time_id IS NULL OR employee_role_id IS NULL OR manager_role_id IS NULL OR add_manager_role_id IS NULL THEN
        RAISE EXCEPTION 'Required emp_type or user_roles rows are missing for employee login seed';
    END IF;

    INSERT INTO users (
        user_login_id,
        user_login_pw,
        company_id,
        emp_type_id,
        role_id,
        employee_id,
        encryption_type,
        login_failures
    )
    SELECT
        format('employee.%s', e.employee_id),
        default_password_hash,
        e.company_id,
        full_time_id,
        employee_role_id,
        e.employee_id,
        0,
        0
    FROM employee e
    WHERE NOT EXISTS (
        SELECT 1
        FROM users u
        WHERE u.employee_id = e.employee_id
          AND u.role_id = employee_role_id
    )
      AND NOT EXISTS (
        SELECT 1
        FROM users u
        WHERE u.user_login_id = format('employee.%s', e.employee_id)
    );

    INSERT INTO users (
        user_login_id,
        user_login_pw,
        company_id,
        emp_type_id,
        role_id,
        employee_id,
        encryption_type,
        login_failures
    )
    WITH chosen_managers AS (
        SELECT DISTINCT ON (candidate.company_id)
            candidate.company_id,
            candidate.employee_id
        FROM (
            SELECT
                u.company_id,
                u.employee_id,
                0 AS priority
            FROM users u
            WHERE u.role_id = manager_role_id
              AND u.employee_id IS NOT NULL
            UNION ALL
            SELECT
                e.company_id,
                e.employee_id,
                1 AS priority
            FROM employee e
        ) candidate
        ORDER BY candidate.company_id, candidate.priority, candidate.employee_id
    )
    SELECT
        format('manager.%s', cm.employee_id),
        default_password_hash,
        cm.company_id,
        full_time_id,
        manager_role_id,
        cm.employee_id,
        0,
        0
    FROM chosen_managers cm
    WHERE NOT EXISTS (
        SELECT 1
        FROM users u
        WHERE u.employee_id = cm.employee_id
          AND u.role_id = manager_role_id
    )
      AND NOT EXISTS (
        SELECT 1
        FROM users u
        WHERE u.user_login_id = format('manager.%s', cm.employee_id)
    );

    INSERT INTO users (
        user_login_id,
        user_login_pw,
        company_id,
        emp_type_id,
        role_id,
        employee_id,
        encryption_type,
        login_failures
    )
    WITH chosen_managers AS (
        SELECT DISTINCT ON (candidate.company_id)
            candidate.company_id,
            candidate.employee_id
        FROM (
            SELECT
                u.company_id,
                u.employee_id,
                0 AS priority
            FROM users u
            WHERE u.role_id = manager_role_id
              AND u.employee_id IS NOT NULL
            UNION ALL
            SELECT
                e.company_id,
                e.employee_id,
                1 AS priority
            FROM employee e
        ) candidate
        ORDER BY candidate.company_id, candidate.priority, candidate.employee_id
    ),
    additional_candidates AS (
        SELECT
            ranked.company_id,
            ranked.employee_id
        FROM (
            SELECT
                candidate.company_id,
                candidate.employee_id,
                row_number() OVER (
                    PARTITION BY candidate.company_id
                    ORDER BY candidate.priority, candidate.employee_id
                ) AS row_number
            FROM (
                SELECT DISTINCT
                    u.company_id,
                    u.employee_id,
                    0 AS priority
                FROM users u
                WHERE u.role_id = add_manager_role_id
                  AND u.employee_id IS NOT NULL
                UNION ALL
                SELECT
                    e.company_id,
                    e.employee_id,
                    1 AS priority
                FROM employee e
            ) candidate
            JOIN chosen_managers cm
              ON cm.company_id = candidate.company_id
            WHERE candidate.employee_id <> cm.employee_id
        ) ranked
        WHERE ranked.row_number <= 5
    )
    SELECT
        format('addmanager.%s', ac.employee_id),
        default_password_hash,
        ac.company_id,
        full_time_id,
        add_manager_role_id,
        ac.employee_id,
        0,
        0
    FROM additional_candidates ac
    WHERE NOT EXISTS (
        SELECT 1
        FROM users u
        WHERE u.employee_id = ac.employee_id
          AND u.role_id = add_manager_role_id
    )
      AND NOT EXISTS (
        SELECT 1
        FROM users u
        WHERE u.user_login_id = format('addmanager.%s', ac.employee_id)
    );

    INSERT INTO manager_permissions (
        user_id,
        is_main_manager,
        can_edit_shifts,
        can_manage_positions,
        can_manage_team_members,
        can_receive_manager_notifications
    )
    SELECT
        u.user_id,
        u.role_id = manager_role_id,
        u.role_id = manager_role_id,
        u.role_id = manager_role_id,
        u.role_id = manager_role_id,
        TRUE
    FROM users u
    WHERE u.role_id IN (manager_role_id, add_manager_role_id)
      AND NOT EXISTS (
        SELECT 1
        FROM manager_permissions mp
        WHERE mp.user_id = u.user_id
    );
END $$;

DO $$
DECLARE
    emp_record RECORD;
    cities TEXT[] := ARRAY['San Francisco', 'New York', 'Los Angeles', 'Chicago', 'Houston', 'Phoenix', 'Philadelphia', 'San Antonio', 'San Diego', 'Dallas'];
    states TEXT[] := ARRAY['CA', 'NY', 'CA', 'IL', 'TX', 'AZ', 'PA', 'TX', 'CA', 'TX'];
    random_idx INT;
BEGIN
    PERFORM set_config('app.internal_system_lookup', 'true', true);

    FOR emp_record IN SELECT employee_id FROM employee LOOP
        random_idx := floor(random() * array_length(cities, 1)) + 1;

        INSERT INTO employee_address (employee_id, address, address2, city, state, zip)
        VALUES (
            emp_record.employee_id,
            (floor(random() * 9000) + 100)::TEXT || ' ' || (ARRAY['Oak', 'Maple', 'Cedar', 'Pine', 'Elm', 'Washington', 'Lincoln', 'Main', 'Broadway', 'Market'])[floor(random() * 10) + 1] || ' St',
            CASE WHEN random() < 0.3 THEN 'Suite ' || (floor(random() * 500) + 1)::TEXT ELSE NULL END,
            cities[random_idx],
            states[random_idx],
            LPAD(floor(random() * 90000 + 10000)::TEXT, 5, '0')
        )
        ON CONFLICT (employee_id) DO UPDATE SET
            address = EXCLUDED.address,
            address2 = EXCLUDED.address2,
            city = EXCLUDED.city,
            state = EXCLUDED.state,
            zip = EXCLUDED.zip;

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

    PERFORM set_config('app.internal_system_lookup', 'false', true);
END $$;

SELECT set_config('app.current_tenant', '1', false);
INSERT INTO day_prefs (employee_id, date, prefs, compression, edited_by, is_day_prefs)
SELECT 1, '2026-04-01'::DATE + i, 'DDDDDDDDDDDDDDDDDDDDPPPPPPPPDDDPPPPPPDDDDPPPPPPPPPPPPPPCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCNNNNN', 0, 1, FALSE
FROM generate_series(0, 30) i;

INSERT INTO week_prefs (employee_id, start_date, prefs, compression, edited_by)
VALUES (1, '2026-04-06', 'DDDDDDDDDDDDDNDDDDDDPPPPPPPPDDDPPPPPPDDDDPPPPPPPPPPPPPPCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPNNNNNNPPPPPPPPPPPPPPPCCCCCCCCCCCCCCCCCCCCPPPPPPPPCPPPPPPPPPPPPPPPPPPPPPPPPCCCPPCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPCCCCCCCCCCCCCCCCCCCCPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPCCPCPPCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDDDDDPPPPPPPPDDDDDDDDDDDDDDDDDPPPPPPPPPPPPPPPPPPPDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP', 0, 1);
