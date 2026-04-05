CREATE SEQUENCE IF NOT EXISTS category_id_seq START WITH 100000;

SELECT setval(
    'category_id_seq',
    GREATEST(COALESCE((SELECT MAX(category_id) FROM category), 0) + 1, 100000),
    false
);

ALTER TABLE category
    ALTER COLUMN category_id SET DEFAULT nextval('category_id_seq');
