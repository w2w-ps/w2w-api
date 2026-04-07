-- Create day_prefs and week_prefs tables

CREATE TABLE day_prefs (
    employee_id INT NOT NULL,
    date DATE NOT NULL,
    prefs CHAR(96),
    compression INT,
    edited_by INT,
    is_day_prefs BOOLEAN DEFAULT FALSE,

    PRIMARY KEY (employee_id, date),

    CONSTRAINT fk_dayprefs_emp
    FOREIGN KEY (employee_id)
    REFERENCES employee(employee_id),

    CONSTRAINT fk_dayprefs_editor
    FOREIGN KEY (edited_by)
    REFERENCES employee(employee_id)
);

CREATE TABLE week_prefs (
    employee_id INT NOT NULL,
    start_date DATE NOT NULL,
    prefs CHAR(672),
    compression INT,
    edited_by INT,

    PRIMARY KEY (employee_id, start_date),

    CONSTRAINT fk_weekprefs_emp
    FOREIGN KEY (employee_id)
    REFERENCES employee(employee_id),

    CONSTRAINT fk_weekprefs_editor
    FOREIGN KEY (edited_by)
    REFERENCES employee(employee_id)
);
