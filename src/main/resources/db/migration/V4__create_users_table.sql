-- USERS table already exists in the target database.
-- No migration needed.
-- Create companies table
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

-- Create users table
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    user_login_id VARCHAR(255) UNIQUE NOT NULL,
    user_login_pw VARCHAR(255) NOT NULL,
    company_id INTEGER,
    worker_type VARCHAR(100),
    encryption_type INTEGER,
    login_failures INTEGER DEFAULT 0,

    CONSTRAINT fk_users_company
        FOREIGN KEY (company_id)
        REFERENCES companies(company_id)
);