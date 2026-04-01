CREATE TABLE emp_type (
    emp_type_id SERIAL  PRIMARY KEY,
    emp_type_name VARCHAR(100) UNIQUE NOT NULL
);


CREATE TABLE user_roles (
    role_id SERIAL  PRIMARY KEY,
    role_name VARCHAR(100) UNIQUE NOT NULL
);
CREATE TABLE companies (
    company_id SERIAL PRIMARY KEY,
    company_name VARCHAR(255),
    department_name VARCHAR(255),
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    created_at TIMESTAMP,
    timezone FLOAT,
    status VARCHAR(50),
    trial_start DATE,
    drop_dead DATE,
    price_table INTEGER,
    tos VARCHAR(255)
);

CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    user_login_id VARCHAR(255) UNIQUE NOT NULL,
    user_login_pw VARCHAR(255) NOT NULL,

    company_id INTEGER,
    emp_type_id INTEGER,
    role_id INTEGER,
    employee_id INTEGER,

    encryption_type INTEGER,
    login_failures INTEGER DEFAULT 0,

    CONSTRAINT fk_users_company
        FOREIGN KEY (company_id)
        REFERENCES companies(company_id),

    CONSTRAINT fk_users_emp_type
        FOREIGN KEY (emp_type_id)
        REFERENCES emp_type(emp_type_id),

    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES user_roles(role_id),

    CONSTRAINT fk_users_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
);