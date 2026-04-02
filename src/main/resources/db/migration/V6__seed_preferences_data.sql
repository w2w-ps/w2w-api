-- Seed data for preferences tables

-- Day preferences for admin (employee_id 1)
INSERT INTO day_prefs (employee_id, date, prefs, compression, edited_by)
VALUES 
(1, '2026-04-01', '111111110000000011111111111111110000000011111111111111110000000011111111111111110000000011111111', 0, 1),
(1, '2026-04-02', '000000001111111100000000111111110000000011111111000000001111111100000000111111110000000011111111', 0, 1);

-- Week preferences for admin (employee_id 1)
INSERT INTO week_prefs (employee_id, start_date, prefs, compression, edited_by)
VALUES 
(1, '2026-03-30', RPAD('WEEKLY_PREF_STRING_FOR_ADMIN', 700, '0'), 0, 1);

-- Seed data for some employees from V2 (if they exist)
-- Assuming employee_ids 2, 3, 4 exist from V2 script
INSERT INTO day_prefs (employee_id, date, prefs, compression, edited_by)
VALUES 
(2, '2026-04-01', RPAD('1', 96, '1'), 0, 1),
(3, '2026-04-01', RPAD('0', 96, '0'), 0, 1);
