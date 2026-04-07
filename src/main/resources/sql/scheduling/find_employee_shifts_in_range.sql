-- Optimization: Build JSON objects once per company position to avoid redundant processing per employee
WITH company_positions AS (
    SELECT
        p.position_id,
        json_build_object('positionId', p.position_id, 'description', p.description) as position_json,
        p.company_id as c_id,
        p.description
    FROM position p
    WHERE p.company_id = :companyId
      AND p.is_deleted = false
),
filtered_employees AS (
    SELECT
        e.employee_id,
        e.first_name,
        e.last_name,
        e.company_id
    FROM employee e
    WHERE e.company_id = :companyId
),
filtered_company AS (
    SELECT company_id AS c_id FROM company WHERE company_id = :companyId
),
filtered_shifts AS (
    SELECT
        se.shift_id,
        se.employee_id,
        sc.start_date AS weekCommencing,
        se.start_time,
        se.end_time,
        se.is_overnight,
        p.description AS position,
        cat.description AS category,
        se.description,
        se.duration,
        se.color
    FROM scheduled_employee se
    JOIN schedule sc ON se.schedule_id = sc.schedule_id
        AND sc.start_date BETWEEN :startDate AND :endDate
    JOIN filtered_company fc ON se.company_id = fc.c_id
    LEFT JOIN position p ON se.required_position_id = p.position_id
    LEFT JOIN category cat ON se.category_id = cat.category_id
    WHERE se.is_deleted = false
),
relevant_employee_ids AS (
    SELECT employee_id FROM filtered_employees
    UNION
    SELECT employee_id FROM filtered_shifts WHERE employee_id IS NOT NULL
),
phone_data AS (
    SELECT
        ep.employee_id,
        string_agg(ep.phone_number, ',' ORDER BY ep.sort_order) AS phones
    FROM employee_phone ep
    WHERE ep.employee_id IN (SELECT employee_id FROM relevant_employee_ids)
    GROUP BY ep.employee_id
),
position_data AS (
    SELECT
        ep.employee_id,
        jsonb_agg(
            cp.position_json
            ORDER BY cp.description, cp.position_id
        ) AS positions
    FROM employee_position ep
    JOIN company_positions cp ON ep.position_id = cp.position_id
    WHERE ep.employee_id IN (SELECT employee_id FROM relevant_employee_ids)
    GROUP BY ep.employee_id
),
employee_shifts AS (
    SELECT
        fs.employee_id,
        jsonb_object_agg(
            fs.weekCommencing,
            jsonb_build_object(
                'date', fs.weekCommencing,
                'shifts', (
                    SELECT jsonb_agg(jsonb_build_object(
                        'shiftId', s.shift_id,
                        'startTime', s.start_time,
                        'endTime', s.end_time,
                        'position', s.position,
                        'category', s.category,
                        'description', s.description,
                        'duration', s.duration,
                        'color', s.color
                    ))
                    FROM filtered_shifts s
                    WHERE s.employee_id = fs.employee_id AND s.weekCommencing = fs.weekCommencing
                )
            )
        ) AS weeklyShifts,
        COUNT(fs.shift_id) AS shiftCount,
        SUM(fs.duration) AS totalDuration
    FROM filtered_shifts fs
    WHERE fs.employee_id IS NOT NULL
    GROUP BY fs.employee_id
)
SELECT
    fe.employee_id,
    fe.first_name,
    fe.last_name,
    pd.phones,
    posd.positions AS availablePositions,
    es.weeklyShifts,
    COALESCE(es.totalDuration, 0) AS totalHours,
    COALESCE(es.shiftCount, 0) AS shiftCount
FROM filtered_employees fe
LEFT JOIN phone_data pd ON fe.employee_id = pd.employee_id
LEFT JOIN employee_shifts es ON fe.employee_id = es.employee_id
LEFT JOIN position_data posd ON fe.employee_id = posd.employee_id
WHERE fe.employee_id IN (SELECT employee_id FROM relevant_employee_ids)
ORDER BY fe.last_name, fe.first_name;
