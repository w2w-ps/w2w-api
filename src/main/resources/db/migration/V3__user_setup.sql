-- User, role, and permission setup
INSERT INTO user_roles (role_name)
VALUES ('Manager'), ('Employee'), ('AddManager')
ON CONFLICT (role_name) DO NOTHING;

DO $$
DECLARE
    default_password_hash CONSTANT TEXT := '$2a$12$9B69QSXuEqf6bgZcWbJXMOc0RHlFkwHQ4iInRtrIwiC9nAJSTgdk.';
    employee_role_id INTEGER;
    manager_role_id INTEGER;
    add_manager_role_id INTEGER;
BEGIN
    PERFORM set_config('app.internal_system_lookup', 'true', true);

    SELECT role_id INTO employee_role_id
    FROM user_roles
    WHERE role_name = 'Employee';

    SELECT role_id INTO manager_role_id
    FROM user_roles
    WHERE role_name = 'Manager';

    SELECT role_id INTO add_manager_role_id
    FROM user_roles
    WHERE role_name = 'AddManager';

    IF employee_role_id IS NULL OR manager_role_id IS NULL OR add_manager_role_id IS NULL THEN
        RAISE EXCEPTION 'Required user_roles rows are missing for user setup';
    END IF;

    INSERT INTO users (
        user_login_id,
        user_login_pw,
        company_id,
        role_id,
        employee_id,
        encryption_type,
        login_failures
    )
    VALUES (
        'admin',
        default_password_hash,
        1,
        manager_role_id,
        1,
        0,
        0
    )
    ON CONFLICT (user_login_id) DO NOTHING;

    INSERT INTO users (
        user_login_id,
        user_login_pw,
        company_id,
        role_id,
        employee_id,
        encryption_type,
        login_failures
    )
    SELECT
        format('employee.%s', e.employee_id),
        default_password_hash,
        e.company_id,
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

    PERFORM set_config('app.internal_system_lookup', 'false', true);
END $$;

-- Application database role setup
DO $$
BEGIN
  IF '${app.db.user}' <> '${flyway.db.user}' THEN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${app.db.user}') THEN
      EXECUTE format(
        'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT NOREPLICATION',
        '${app.db.user}',
        '${app.db.password}'
      );
    ELSE
      EXECUTE format(
        'ALTER ROLE %I WITH LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT NOREPLICATION',
        '${app.db.user}',
        '${app.db.password}'
      );
    END IF;

    EXECUTE format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), '${app.db.user}');
    EXECUTE format('REVOKE CREATE ON SCHEMA public FROM %I', '${app.db.user}');
    EXECUTE format('REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM %I', '${app.db.user}');
    EXECUTE format('REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM %I', '${app.db.user}');
    EXECUTE format('REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM %I', '${app.db.user}');

    EXECUTE format('GRANT USAGE ON SCHEMA public TO %I', '${app.db.user}');
    EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO %I', '${app.db.user}');
    EXECUTE format('GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO %I', '${app.db.user}');
    EXECUTE format('GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO %I', '${app.db.user}');

    EXECUTE format(
      'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I',
      '${flyway.db.user}',
      '${app.db.user}'
    );
    EXECUTE format(
      'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO %I',
      '${flyway.db.user}',
      '${app.db.user}'
    );
    EXECUTE format(
      'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT EXECUTE ON FUNCTIONS TO %I',
      '${flyway.db.user}',
      '${app.db.user}'
    );
  END IF;
END $$;
