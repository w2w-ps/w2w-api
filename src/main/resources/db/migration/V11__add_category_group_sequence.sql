CREATE SEQUENCE IF NOT EXISTS category_group_id_seq START WITH 100000;

SELECT setval(
    'category_group_id_seq',
    GREATEST(COALESCE((SELECT MAX(group_id) FROM cat_group), 0) + 1, 100000),
    false
);

ALTER TABLE cat_group
    ALTER COLUMN group_id SET DEFAULT nextval('category_group_id_seq');
