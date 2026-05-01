UPDATE category
SET short_desc = COALESCE(NULLIF(btrim(description), ''), 'Category ' || category_id)
WHERE short_desc IS NULL
   OR btrim(short_desc) = '';

ALTER TABLE category
  ALTER COLUMN short_desc SET NOT NULL;

ALTER TABLE category
  ADD CONSTRAINT chk_category_short_desc_not_blank
  CHECK (btrim(short_desc) <> '');
