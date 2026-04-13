-- Backfill and seed category.short_desc values for scheduling responses that now
-- return only short category descriptions.

UPDATE category
SET short_desc = CASE
    WHEN short_desc IS NOT NULL AND btrim(short_desc) <> '' THEN short_desc
    WHEN description = 'Morning Shift' THEN 'MORN'
    WHEN description = 'Afternoon Shift' THEN 'AFT'
    WHEN description = 'Night Shift' THEN 'NGT'
    WHEN description = 'Weekend Shift' THEN 'WKND'
    WHEN description = 'Emergency' THEN 'EMRG'
    WHEN description = 'Holiday Shift' THEN 'HOL'
    WHEN description = 'On-Call' THEN 'CALL'
    WHEN description = 'Split Shift' THEN 'SPLT'
    WHEN description = 'Graveyard' THEN 'GRVY'
    WHEN description = 'Training' THEN 'TRNG'
    ELSE upper(left(regexp_replace(coalesce(description, ''), '[^A-Za-z0-9]', '', 'g'), 4))
END
WHERE short_desc IS NULL OR btrim(short_desc) = '';
