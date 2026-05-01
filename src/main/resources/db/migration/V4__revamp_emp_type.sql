ALTER TABLE emp_type RENAME COLUMN emp_type_id TO id;
ALTER TABLE emp_type RENAME COLUMN emp_type_name TO name;

ALTER TABLE emp_type ADD COLUMN display_name VARCHAR(100);
ALTER TABLE emp_type ADD COLUMN sort_order INTEGER;

INSERT INTO emp_type (id, name, display_name, sort_order)
VALUES (4, '__per_diem_remap__', 'Per Diem', 3)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    display_name = EXCLUDED.display_name,
    sort_order = EXCLUDED.sort_order;

UPDATE employee
SET emp_type_id = 4
WHERE emp_type_id = 3;

UPDATE users
SET emp_type_id = 4
WHERE emp_type_id = 3;

INSERT INTO emp_type (id, name, display_name, sort_order)
VALUES
    (1, 'Purple Diamond', 'Full Time', 1),
    (2, 'Blue Diamond', 'Part Time', 2),
    (5, 'Green Diamond', NULL, 5)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    display_name = EXCLUDED.display_name,
    sort_order = EXCLUDED.sort_order;

UPDATE emp_type
SET name = 'Orange Diamond',
    display_name = NULL,
    sort_order = 4
WHERE id = 3;

UPDATE emp_type
SET name = 'Red Diamond',
    display_name = 'Per Diem',
    sort_order = 3
WHERE id = 4;

ALTER TABLE emp_type ALTER COLUMN sort_order SET NOT NULL;

SELECT setval(pg_get_serial_sequence('emp_type', 'id'), (SELECT MAX(id) FROM emp_type), true);
