# AGENTS.md

## Architecture
- Stack: Spring Boot 4, Spring Web, Spring Data JPA, PostgreSQL, Flyway.
- Entry point: `src/main/java/com/w2w/api/W2WApplication.java`.
- Multi-tenant behavior is request-driven through `TenantContext` and `TenantDatabaseConfig`.
- Flyway migrations live in `src/main/resources/db/migration`.
- Custom SQL for scheduling lives in `src/main/resources/sql/scheduling`.

## Feature Layout
- Keep each feature under `src/main/java/com/w2w/api/<feature>`.
- Keep `Controller` and `Service` classes at the feature root.
- Group supporting types into subfolders only when there are multiple files:
  `dto/`, `model/`, `repository/`.
- Current features:
  `tenant`, `employee`, `position`, `positiongroup`, `category`, `categorygroup`, `scheduling`, `config`, `preferences`.

## API Surface
- `tenant`
  `GET /api/tenants`
  `GET /api/tenants/{id}`
  `POST /api/tenants`
- `employee`
  `GET /api/employees`
  `GET /api/employees/{id}` full employee detail view including assigned positions, username, accessibility mode, autofill options, and comment
  `POST /api/employees`
  `PUT /api/employees/{id}`
  `PATCH /api/employees/{id}` partial update
  `DELETE /api/employees/{id}`
  `GET /api/employees/config`
  `PATCH /api/employees/config`
- `position`
  `GET /api/positions?companyId=...&status=...`
  `GET /api/positions/{id}?companyId=...`
  `POST /api/positions`
  `PUT /api/positions/{id}?companyId=...`
  `DELETE /api/positions/{id}?companyId=...`
  `POST /api/positions/{id}/restore?companyId=...`
- `positiongroup`
  `GET /api/position-groups?status=...`
  `GET /api/position-groups/{id}`
  `POST /api/position-groups`
  `PUT /api/position-groups/{id}`
  `DELETE /api/position-groups/{id}`
- `category`
  `GET /api/categories?status=...` default `active`
  `GET /api/categories/{id}`
  `POST /api/categories`
  `PUT /api/categories/{id}`
  `DELETE /api/categories/{id}`
- `categorygroup`
  `GET /api/category-groups?status=...`
  `GET /api/category-groups/{id}`
  `POST /api/category-groups`
  `PUT /api/category-groups/{id}`
  `DELETE /api/category-groups/{id}`
- `scheduling`
  `POST /api/scheduling/shifts` accepts numeric `color` ID
  `GET /api/scheduling/shifts/{shiftId}` returns numeric `color` ID
  `PUT /api/scheduling/shifts/{shiftId}` returns numeric `color` ID
  `GET /api/scheduling/shift-colors`
  `GET /api/scheduling/shifts/employees?companyId=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd&positionIds=...&categoryIds=...` includes nullable `employmentType`, `empTypeId`, `alertDate`, and `publishedStage`; filters keep all visible shifts for matched employees while totals count only matching positions and categories outside supplied `categoryIds`
  `GET /api/scheduling/shifts/grouped?companyId=...&grouping=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd&positionIds=...&categoryIds=...` grouped calendar response with optional position/category filtering; shift items include nullable `empTypeId`; position grouping sorts by position, start time, last name, first name; category/CAT grouping sorts no/null category first, then category/CAT label, start time, last name, first name; shift timing grouping sorts by start time, last name, first name
  `GET /api/scheduling/shifts/date-position?companyId=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd&positionIds=...&categoryIds=...` wrapped response with `title`, `totalShifts`, `totalHours`, and `dates[{ weekday, date, ... }]`; shift items include nullable `employmentType` and `empTypeId`; shifts sort by start time, last name, first name
  `GET /api/scheduling/employees?companyId=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd&positionIds=...&categoryIds=...` deprecated
- `timeoff`
  `GET /api/time-off/requests?employeeId=...&status=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd`
  `POST /api/time-off/requests` accepts `employeeId`; managers can create for any employee
  `PUT /api/time-off/requests/{requestId}/approve` managers can approve or decline pending requests
  `PUT /api/time-off/requests/{requestId}/cancel`
- `preferences`
  `GET /api/preferences/day?employeeId=...&date=...&companyId=...`
  `GET /api/preferences/day/range?employeeId=...&startDate=...&endDate=...&companyId=...`
  `GET /api/preferences/resolved?employeeId=...&startDate=...&endDate=...&companyId=...`
  `POST /api/preferences/day`
  `POST /api/preferences/day/repeat`
  `GET /api/preferences/week?employeeId=...&startDate=...&companyId=...`
  `POST /api/preferences/week`

## Tenanting Rules
- Reads and writes are tenant-scoped by setting `TenantContext` before repository access.
- Controllers currently own tenant selection unless the feature explicitly derives tenant scope from authentication.
- Category, categorygroup, and positiongroup now rely on the authenticated tenant rather than request `companyId`.
- For request params, use the provided `companyId` where the contract still exposes it.
- For create endpoints, use the authenticated tenant unless the contract explicitly requires a tenant id in the payload.

## Data and Query Notes
- `schema.sql` is a checked-in schema snapshot and should stay aligned with Flyway migrations.
- Scheduling grouped results are built from a custom SQL query plus service-level grouping logic.
- Shift `color` is stored as `SMALLINT`; create/update and single-shift responses use numeric IDs, while scheduling calendar/list APIs return color strings and resolve null/unknown IDs to `black`.
- Shift color IDs follow the legacy dropdown palette: `0=black`, `1=brown`, `2=blue`, `3=fuchsia`, `4=gray`, `5=green`, `6=navy`, `7=orange`, `8=purple`, `9=red`, `10=turquoise`, `11=lavender`, `12=lime`, `13=salmon`, `14=gold`, `15=aqua`, `16=maroon`.
- Scheduling employee view shows open/unassigned shifts with or without filters; supplied position/category filters apply to the open shift's position/category, and employee totals never count unassigned shifts.
- Grouped scheduling response uses day buckets relative to the requested `startDate`.
- Scheduling grouped/date-position name sorting puts null names before non-null names.
- Each day bucket carries the bucket date; individual shifts do not repeat that date.

## Testing
- Existing automated coverage is under `src/test/java/com/w2w/api/scheduling`.
- Run `mvn -q test` before pushing backend changes; if quiet-mode tests fail, rerun `mvn test` for full diagnostics.
- If changing scheduling query shape or grouping logic, update both service tests and repository mapping tests.

## Change Guidelines
- Preserve the feature-first package layout.
- Do not reintroduce `controller/` or `service/` subpackages for category or position.
- Keep DTO and model types inside their owning feature package, using `dto/` and `model/` subpackages when needed.
- Keep DTO/model/repository folders grouped only when they contain multiple files.
- Name top-level inbound payloads `*Request` and top-level outbound payloads `*Response`.
- Name reusable nested API DTOs by business role rather than `*Dto`; prefer semantic suffixes such as `*Summary`, `*Detail`, `*Reference`, `*Bucket`, or `*Item`.
- Keep repository and query-mapping types on `*Projection`.
- Prefer Java `record`s for DTOs whenever possible. Use a class only when mutability, framework binding, or helper methods make a record a poor fit.
- Add new database changes as incremental Flyway migrations; do not edit already-applied migrations in a shared environment.
- When request or response contracts change, update `openapi.yaml`.
- When request or response contracts change, update request examples in `postman/w2w-api.postman_collection.json`.
- Any new or changed APIs must be documented in the `API Surface` section of this file and added/updated in the `postman/w2w-api.postman_collection.json` file.
- Keep the database schema and seed data in `src/main/resources/db/migration` in sync with any model changes.

## Sonar Guidelines
- Keep required public JPA no-arg constructors; add `// Required by JPA for entity instantiation.` when Sonar flags an empty body.
- Do not hard-code default passwords; generate initial passwords with `InitialAccountPasswordGenerator` and encode with `PasswordEncoder`.
- Avoid dynamic SQL string building; use bind parameters, including PostgreSQL `set_config(..., ?, false)` for session settings.
- Avoid regexes with backtracking risk on user input; prefer simple linear scans for password complexity checks.
- For duplicated literals or complex methods, use small constants/helpers and add focused tests for preserved behavior.
