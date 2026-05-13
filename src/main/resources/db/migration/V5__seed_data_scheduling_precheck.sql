-- ============================================================
-- Seed Data for Scheduling Pre-Check Scenarios
-- ============================================================
SELECT set_config('app.current_tenant', '1', false);

DO $$
DECLARE
    emp1_id INT;
    emp2_id INT;
    emp3_id INT;
    emp4_id INT;
    emp5_id INT;
    emp6_id INT;
    emp7_id INT;
    sched_id INT;
    pos_id INT;
    cat_id INT;
BEGIN
    SELECT position_id INTO pos_id FROM position WHERE company_id = 1 LIMIT 1;
    SELECT category_id INTO cat_id FROM category WHERE company_id = 1 LIMIT 1;


    INSERT INTO schedule (company_id, description, start_date, day_of_week)
    VALUES (1, 'Pre-check Schedule (Sat)', '2026-06-27', 6)
    RETURNING schedule_id INTO sched_id;

    -- Scenario 1: MaxDailyHoursRule
    emp1_id := 90001;
    INSERT INTO employee (employee_id, company_id, first_name, last_name, status, max_daily_hours)
    VALUES (emp1_id, 1, 'MaxDailyHours', 'Precheck', 'active', 8);
    INSERT INTO employee_position (employee_id, position_id) VALUES (emp1_id, pos_id);

    INSERT INTO scheduled_employee (employee_id, schedule_id, company_id, start_time, end_time, duration, required_position_id, category_id)
    VALUES (emp1_id, sched_id, 1, '08:00:00', '14:00:00', 6.0, pos_id, cat_id);

    -- Scenario 2: MaxDailyShiftsRule
    emp2_id := 90002;
    INSERT INTO employee (employee_id, company_id, first_name, last_name, status, max_daily_shifts)
    VALUES (emp2_id, 1, 'MaxDailyShifts', 'Precheck', 'active', 2);
    INSERT INTO employee_position (employee_id, position_id) VALUES (emp2_id, pos_id);

    INSERT INTO scheduled_employee (employee_id, schedule_id, company_id, start_time, end_time, duration, required_position_id, category_id)
    VALUES (emp2_id, sched_id, 1, '08:00:00', '10:00:00', 2.0, pos_id, cat_id);
    
    INSERT INTO scheduled_employee (employee_id, schedule_id, company_id, start_time, end_time, duration, required_position_id, category_id)
    VALUES (emp2_id, sched_id, 1, '12:00:00', '14:00:00', 2.0, pos_id, cat_id);

    -- Scenario 3: MaxWeeklyHoursAndShiftsRule
    emp3_id := 90003;
    INSERT INTO employee (employee_id, company_id, first_name, last_name, status, max_scheduled_hours, max_weekly_days)
    VALUES (emp3_id, 1, 'MaxWeekly', 'Precheck', 'active', 40, 5);
    INSERT INTO employee_position (employee_id, position_id) VALUES (emp3_id, pos_id);

    FOR i IN 0..4 LOOP
        INSERT INTO schedule (company_id, description, start_date, day_of_week)
        VALUES (1, 'Pre-check Schedule (Weekday ' || i || ')', '2026-06-22'::DATE + i, 1 + i)
        RETURNING schedule_id INTO sched_id;
        
        INSERT INTO scheduled_employee (employee_id, schedule_id, company_id, start_time, end_time, duration, required_position_id, category_id)
        VALUES (emp3_id, sched_id, 1, '08:00:00', '16:00:00', 8.0, pos_id, cat_id);
    END LOOP;

    -- Scenario 4: WorkPreferencesRule
    emp4_id := 90004;
    INSERT INTO employee (employee_id, company_id, first_name, last_name, status)
    VALUES (emp4_id, 1, 'WorkPrefs', 'Precheck', 'active');
    INSERT INTO employee_position (employee_id, position_id) VALUES (emp4_id, pos_id);

    INSERT INTO day_prefs (employee_id, date, prefs, compression, is_day_prefs)
    VALUES (emp4_id, '2026-06-27', 'PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPCCCCCCCCCCCCCCCCPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP', 0, TRUE);

    -- Scenario 5: TimeOffRule
    emp5_id := 90005;
    INSERT INTO employee (employee_id, company_id, first_name, last_name, status)
    VALUES (emp5_id, 1, 'TimeOff', 'Precheck', 'active');
    INSERT INTO employee_position (employee_id, position_id) VALUES (emp5_id, pos_id);

    INSERT INTO time_off_request (company_id, employee_id, start_date, end_date, start_time, end_time, status, is_full_day)
    VALUES (1, emp5_id, '2026-06-27', '2026-06-27', '09:00:00', '17:00:00', 'approved', false);

    -- Scenario 6: ExistingShiftConflictRule
    emp6_id := 90006;
    INSERT INTO employee (employee_id, company_id, first_name, last_name, status)
    VALUES (emp6_id, 1, 'ShiftConflict', 'Precheck', 'active');
    INSERT INTO employee_position (employee_id, position_id) VALUES (emp6_id, pos_id);

    SELECT schedule_id INTO sched_id FROM schedule WHERE start_date = '2026-06-27' AND company_id = 1 LIMIT 1;

    INSERT INTO scheduled_employee (shift_id, employee_id, schedule_id, company_id, start_time, end_time, duration, required_position_id, category_id)
    VALUES (900006, emp6_id, sched_id, 1, '08:00:00', '16:00:00', 8.0, pos_id, cat_id);

    -- Scenario 8: Overnight Conflict Coverage
    emp7_id := 90007;
    INSERT INTO employee (employee_id, company_id, first_name, last_name, status, max_daily_hours)
    VALUES (emp7_id, 1, 'Overnight', 'Precheck', 'active', 8);
    INSERT INTO employee_position (employee_id, position_id) VALUES (emp7_id, pos_id);

    INSERT INTO schedule (company_id, description, start_date, day_of_week)
    VALUES (1, 'Pre-check Schedule (Fri)', '2026-06-26', 5)
    RETURNING schedule_id INTO sched_id;

    INSERT INTO scheduled_employee (employee_id, schedule_id, company_id, start_time, end_time, duration, is_overnight, required_position_id, category_id)
    VALUES (emp7_id, sched_id, 1, '22:00:00', '06:00:00', 8.0, TRUE, pos_id, cat_id);

END $$;
