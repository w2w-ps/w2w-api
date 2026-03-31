ALTER TABLE schedule
ALTER COLUMN published TYPE BOOLEAN
USING CASE
    WHEN published IS NULL THEN NULL
    WHEN LOWER(published) = 'true' THEN TRUE
    WHEN LOWER(published) = 'false' THEN FALSE
    ELSE NULL
END;
