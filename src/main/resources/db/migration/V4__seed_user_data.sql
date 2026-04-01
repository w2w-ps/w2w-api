-- Seed data for security tables

INSERT INTO companies (company_name)
VALUES ('Default Company');
INSERT INTO emp_type (emp_type_name) VALUES ('ADMIN');

-- role
INSERT INTO user_roles (role_name) VALUES ('ROLE_ADMIN');

-- user
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
