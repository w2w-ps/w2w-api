-- Complete Database Schema

CREATE SEQUENCE scheduled_employee_shift_id_seq START WITH 100000;
CREATE SEQUENCE schedule_id_seq START WITH 100000;

CREATE TABLE company (
  company_id INTEGER PRIMARY KEY,
  company_name VARCHAR(255),
  department_name VARCHAR(255),
  address VARCHAR(255),
  city VARCHAR(255),
  state VARCHAR(255),
  timestamp TIMESTAMP,
  timezone FLOAT,
  status VARCHAR(255),
  trial_start DATE,
  drop_dead DATE,
  price_table INTEGER,
  tos VARCHAR(255)
);

CREATE TABLE employee (
  employee_id INTEGER PRIMARY KEY,
  company_id INTEGER NOT NULL REFERENCES company(company_id),
  status VARCHAR(255),
  last_logon TIMESTAMP,
  logon_count INTEGER,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  employee_number VARCHAR(255),
  email VARCHAR(255),
  hire_date TIMESTAMP,
  max_scheduled_hours INTEGER,
  max_daily_hours INTEGER,
  pay_rate FLOAT
);

CREATE TABLE employee_phone (
  employee_id INTEGER NOT NULL REFERENCES employee(employee_id) ON DELETE CASCADE,
  sort_order INTEGER NOT NULL,
  phone_number TEXT NOT NULL,
  PRIMARY KEY (employee_id, sort_order)
);

CREATE TABLE schedule (
  schedule_id INTEGER PRIMARY KEY DEFAULT nextval('schedule_id_seq'),
  company_id INTEGER NOT NULL REFERENCES company(company_id),
  is_published BOOLEAN,
  description VARCHAR(255),
  start_date DATE,
  day_of_week SMALLINT,
  timestamp TIMESTAMP,
  last_change TIMESTAMP,
  archived VARCHAR(255)
);

CREATE TABLE skill (
  skill_id INTEGER PRIMARY KEY,
  company_id INTEGER NOT NULL REFERENCES company(company_id),
  description VARCHAR(255),
  status VARCHAR(255),
  timestamp TIMESTAMP
);

CREATE TABLE skill_group (
  group_id INTEGER PRIMARY KEY,
  company_id INTEGER NOT NULL REFERENCES company(company_id),
  description VARCHAR(255)
);

CREATE TABLE group_skill (
  group_id INTEGER NOT NULL REFERENCES skill_group(group_id),
  skill_id INTEGER NOT NULL REFERENCES skill(skill_id),
  PRIMARY KEY (group_id, skill_id)
);

CREATE TABLE category (
  category_id INTEGER PRIMARY KEY,
  company_id INTEGER NOT NULL REFERENCES company(company_id),
  short_desc VARCHAR(255),
  description VARCHAR(255),
  start_time VARCHAR(255),
  end_time VARCHAR(255),
  skill_id INTEGER REFERENCES skill(skill_id),
  color SMALLINT
);

CREATE TABLE cat_group (
  group_id INTEGER PRIMARY KEY,
  company_id INTEGER NOT NULL REFERENCES company(company_id),
  description VARCHAR(255)
);

CREATE TABLE group_cat (
  group_id INTEGER NOT NULL REFERENCES cat_group(group_id),
  cat_id INTEGER NOT NULL REFERENCES category(category_id),
  PRIMARY KEY (group_id, cat_id)
);

CREATE TABLE scheduled_employee (
  shift_id INTEGER PRIMARY KEY DEFAULT nextval('scheduled_employee_shift_id_seq'),
  employee_id INTEGER REFERENCES employee(employee_id),
  schedule_id INTEGER REFERENCES schedule(schedule_id),
  company_id INTEGER REFERENCES company(company_id),
  description VARCHAR(255),
  start_time TIME,
  end_time TIME,
  duration FLOAT,
  is_overnight BOOLEAN DEFAULT FALSE,
  required_skill_id INTEGER REFERENCES skill(skill_id),
  category_id INTEGER REFERENCES category(category_id),
  color VARCHAR(255),
  is_deleted BOOLEAN DEFAULT FALSE,
  changed_by INTEGER REFERENCES employee(employee_id)
);

CREATE TABLE employee_skill (
  employee_id INTEGER NOT NULL REFERENCES employee(employee_id),
  skill_id INTEGER NOT NULL REFERENCES skill(skill_id),
  suitability_level INTEGER,
  preference VARCHAR(255),
  pay_rate FLOAT,
  cert_date DATE,
  PRIMARY KEY (employee_id, skill_id)
);

CREATE TABLE "user" (
  user_id INTEGER PRIMARY KEY,
  user_login_id VARCHAR(255),
  user_login_pw VARCHAR(255),
  company_id INTEGER REFERENCES company(company_id),
  worker_type VARCHAR(255),
  encryption_type INTEGER,
  login_failures INTEGER
);

CREATE INDEX idx_schedule_start_date
  ON schedule (start_date);
