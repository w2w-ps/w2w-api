DO $$
DECLARE
    default_password_hash CONSTANT TEXT := '$2a$12$9B69QSXuEqf6bgZcWbJXMOc0RHlFkwHQ4iInRtrIwiC9nAJSTgdk.';
    full_time_id INTEGER;
    employee_role_id INTEGER;
    manager_role_id INTEGER;
    add_manager_role_id INTEGER;
BEGIN
    INSERT INTO companies (company_id, company_name, department_name, status)
    SELECT
        c.company_id,
        c.company_name,
        c.department_name,
        c.status
    FROM company c
    ON CONFLICT (company_id) DO UPDATE
    SET
        company_name = EXCLUDED.company_name,
        department_name = EXCLUDED.department_name,
        status = EXCLUDED.status;

    PERFORM setval(
            pg_get_serial_sequence('companies', 'company_id'),
            COALESCE((SELECT MAX(company_id) FROM companies), 1),
            true
    );

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
        RAISE EXCEPTION 'Required emp_type or user_roles rows are missing for employee login backfill';
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
        true
    FROM users u
    WHERE u.role_id IN (manager_role_id, add_manager_role_id)
      AND NOT EXISTS (
        SELECT 1
        FROM manager_permissions mp
        WHERE mp.user_id = u.user_id
    );
END $$;
