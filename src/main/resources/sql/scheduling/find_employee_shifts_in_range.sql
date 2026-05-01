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
        e.emp_type_id,
        e.next_alert_date,
        e.company_id
    FROM employee e
    WHERE e.company_id = :companyId
),
filtered_company AS (
    SELECT company_id AS c_id FROM company WHERE company_id = :companyId
),
all_shifts AS (
    SELECT
        se.shift_id,
        se.employee_id,
        sc.start_date AS weekCommencing,
        sc.is_published AS schedulePublished,
        se.start_time,
        se.end_time,
        se.is_overnight,
        se.required_position_id AS positionId,
        p.description AS position,
        se.category_id AS categoryId,
        cat.description AS category,
        cat.short_desc AS categoryShortDescription,
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
filtered_shifts AS (
    SELECT *
    FROM all_shifts
    WHERE (:positionFilterEnabled = false OR positionId IN (:positionIds))
      AND (:categoryFilterEnabled = false OR categoryId IN (:categoryIds))
),
visible_shifts AS (
    SELECT shifts.*
    FROM all_shifts shifts
    WHERE shifts.employee_id IN (
        SELECT employee_id
        FROM filtered_employees
        WHERE :positionFilterEnabled = false
           OR EXISTS (
               SELECT 1
               FROM employee_position ep
               WHERE ep.employee_id = filtered_employees.employee_id
                 AND ep.position_id IN (:positionIds)
           )
    )
    UNION
    SELECT shifts.*
    FROM filtered_shifts shifts
    WHERE shifts.employee_id IS NOT NULL
    UNION ALL
    SELECT shifts.*
    FROM filtered_shifts shifts
    WHERE shifts.employee_id IS NULL
),
relevant_employee_ids AS (
    SELECT employee_id
    FROM filtered_employees
    WHERE :positionFilterEnabled = false
       OR EXISTS (
           SELECT 1
           FROM employee_position ep
           WHERE ep.employee_id = filtered_employees.employee_id
             AND ep.position_id IN (:positionIds)
       )
    UNION
    SELECT employee_id FROM filtered_shifts WHERE employee_id IS NOT NULL
),
unassigned_shifts AS (
    SELECT *
    FROM visible_shifts
    WHERE employee_id IS NULL
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
    fe.emp_type_id AS "empTypeId",
    fe.next_alert_date AS "alertDate",
    pd.phones AS "phones",
    posd.positions AS "availablePositions",
    vs.shift_id AS "shiftId",
    vs.weekCommencing AS "weekCommencing",
    vs.start_time AS "startTime",
    vs.end_time AS "endTime",
    vs.is_overnight AS "isOvernight",
    vs.positionId AS "positionId",
    vs.position AS "position",
    vs.categoryId AS "categoryId",
    vs.category AS "category",
    vs.categoryShortDescription AS "categoryShortDescription",
    vs.description AS "description",
    vs.duration AS "duration",
    vs.schedulePublished AS "schedulePublished",
    vs.color AS "color"
FROM filtered_employees fe
LEFT JOIN phone_data pd ON fe.employee_id = pd.employee_id
LEFT JOIN position_data posd ON fe.employee_id = posd.employee_id
LEFT JOIN visible_shifts vs ON fe.employee_id = vs.employee_id
WHERE fe.employee_id IN (SELECT employee_id FROM relevant_employee_ids)
UNION ALL
SELECT
    NULL AS "employeeId",
    NULL AS "firstName",
    NULL AS "lastName",
    NULL AS "empTypeId",
    NULL AS "alertDate",
    NULL AS "phones",
    NULL AS "availablePositions",
    us.shift_id AS "shiftId",
    us.weekCommencing AS "weekCommencing",
    us.start_time AS "startTime",
    us.end_time AS "endTime",
    us.is_overnight AS "isOvernight",
    us.positionId AS "positionId",
    us.position AS "position",
    us.categoryId AS "categoryId",
    us.category AS "category",
    us.categoryShortDescription AS "categoryShortDescription",
    us.description AS "description",
    us.duration AS "duration",
    us.schedulePublished AS "schedulePublished",
    us.color AS "color"
FROM unassigned_shifts us
ORDER BY "lastName" NULLS FIRST, "firstName" NULLS FIRST, "weekCommencing", "startTime";
