WITH filtered_company AS (
    SELECT :companyId AS c_id
),
-- Optimization: Build JSON objects once per company skill to avoid redundant processing per employee
company_skills AS (
    SELECT
        sk.skill_id,
        json_build_object('positionId', sk.skill_id, 'description', sk.description) as skill_json,
        sk.description
    FROM skill sk
    JOIN filtered_company fc ON sk.company_id = fc.c_id
),
filtered_employees AS (
    SELECT e.employee_id, e.first_name, e.last_name
    FROM employee e
    JOIN filtered_company fc ON e.company_id = fc.c_id
),
filtered_shifts AS (
    SELECT
        se.shift_id,
        se.employee_id,
        sc.start_date AS weekCommencing,
        se.start_time,
        se.end_time,
        se.is_overnight,
        sk.description AS position,
        cat.description AS category,
        se.description,
        se.duration,
        se.color
    FROM scheduled_employee se
    JOIN schedule sc ON se.schedule_id = sc.schedule_id
        AND sc.start_date BETWEEN :startDate AND :endDate
    JOIN filtered_company fc ON se.company_id = fc.c_id
    LEFT JOIN skill sk ON se.required_skill_id = sk.skill_id
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
skill_data AS (
    SELECT
        es.employee_id,
        json_agg(
            cs.skill_json
            ORDER BY cs.description, cs.skill_id
        ) AS availablePositions
    FROM employee_skill es
    JOIN company_skills cs ON es.skill_id = cs.skill_id
    WHERE es.employee_id IN (SELECT employee_id FROM relevant_employee_ids)
    GROUP BY es.employee_id
)
SELECT
    fs.shift_id AS shiftId,
    COALESCE(fe.employee_id, fs.employee_id) AS employeeId,
    fe.first_name AS firstName,
    fe.last_name AS lastName,
    pd.phones AS phones,
    sd.availablePositions AS availablePositions,
    fs.weekCommencing AS weekCommencing,
    fs.start_time AS startTime,
    fs.end_time AS endTime,
    fs.is_overnight AS isOvernight,
    fs.position AS position,
    fs.category AS category,
    fs.description AS description,
    fs.duration AS duration,
    fs.color AS color
FROM filtered_shifts fs
FULL OUTER JOIN filtered_employees fe ON fs.employee_id = fe.employee_id
LEFT JOIN phone_data pd ON COALESCE(fe.employee_id, fs.employee_id) = pd.employee_id
LEFT JOIN skill_data sd ON COALESCE(fe.employee_id, fs.employee_id) = sd.employee_id
ORDER BY employeeId, fs.weekCommencing, fs.start_time;
