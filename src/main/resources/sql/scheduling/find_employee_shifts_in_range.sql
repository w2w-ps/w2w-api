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
)
SELECT
    fe.employee_id AS "employeeId",
    fe.first_name AS "firstName",
    fe.last_name AS "lastName",
    pd.phones AS "phones",
    posd.positions AS "availablePositions",
    fs.shift_id AS "shiftId",
    fs.weekCommencing AS "weekCommencing",
    fs.start_time AS "startTime",
    fs.end_time AS "endTime",
    fs.is_overnight AS "isOvernight",
    fs.position AS "position",
    fs.category AS "category",
    fs.description AS "description",
    fs.duration AS "duration",
    fs.color AS "color"
FROM filtered_employees fe
LEFT JOIN phone_data pd ON fe.employee_id = pd.employee_id
LEFT JOIN position_data posd ON fe.employee_id = posd.employee_id
LEFT JOIN filtered_shifts fs ON fe.employee_id = fs.employee_id
WHERE fe.employee_id IN (SELECT employee_id FROM relevant_employee_ids)
ORDER BY fe.last_name, fe.first_name, fs.weekCommencing, fs.start_time;
