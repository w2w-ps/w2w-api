CREATE SEQUENCE IF NOT EXISTS skill_group_id_seq START WITH 100000;

SELECT setval(
    'skill_group_id_seq',
    GREATEST(COALESCE((SELECT MAX(group_id) FROM skill_group), 0) + 1, 100000),
    false
);

ALTER TABLE skill_group
    ALTER COLUMN group_id SET DEFAULT nextval('skill_group_id_seq');
