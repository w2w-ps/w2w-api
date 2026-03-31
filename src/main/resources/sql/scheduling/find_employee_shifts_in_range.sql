SELECT
    shift_data.shiftId AS shiftId,
    e.employee_id AS employeeId,
    e.first_name AS firstName,
    e.last_name AS lastName,
    phone_data.phones AS phones,
    skill_data.availablePositions AS availablePositions,
    shift_data.weekCommencing AS weekCommencing,
    shift_data.startTime AS startTime,
    shift_data.endTime AS endTime,
    shift_data.isOvernight AS isOvernight,
    shift_data.position AS position,
    shift_data.category AS category,
    shift_data.description AS description,
    shift_data.duration AS duration,
    shift_data.color AS color
FROM employee e
LEFT JOIN (
    SELECT
        employee_id,
        string_agg(phone_number, ',' ORDER BY sort_order) AS phones
    FROM employee_phone
    GROUP BY employee_id
) phone_data ON e.employee_id = phone_data.employee_id
LEFT JOIN (
    SELECT
        es.employee_id,
        json_agg(
            json_build_object(
                'id', sk.skill_id,
                'name', sk.description
            )
            ORDER BY sk.description, sk.skill_id
        ) AS availablePositions
    FROM employee_skill es
    JOIN skill sk ON es.skill_id = sk.skill_id
    GROUP BY es.employee_id
) skill_data ON e.employee_id = skill_data.employee_id
LEFT JOIN (
    SELECT
        se.shift_id AS shiftId,
        se.employee_id AS employeeId,
        sc.start_date AS weekCommencing,
        se.start_time AS startTime,
        se.end_time AS endTime,
        se.is_overnight AS isOvernight,
        sk.description AS position,
        cat.description AS category,
        se.description AS description,
        se.duration AS duration,
        se.color AS color
    FROM scheduled_employee se
    JOIN schedule sc ON se.schedule_id = sc.schedule_id
        AND sc.start_date BETWEEN :startDate AND :endDate
    LEFT JOIN skill sk ON se.required_skill_id = sk.skill_id
    LEFT JOIN category cat ON se.category_id = cat.category_id
    WHERE se.company_id = :companyId
        AND se.is_deleted = false
) shift_data ON e.employee_id = shift_data.employeeId
WHERE e.company_id = :companyId
ORDER BY e.employee_id, shift_data.weekCommencing, shift_data.startTime
