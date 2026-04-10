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
  `tenant`, `employee`, `position`, `positiongroup`, `category`, `scheduling`, `config`, `preferences`.

## API Surface
- `tenant`
  `GET /api/tenants`
  `GET /api/tenants/{id}`
  `POST /api/tenants`
- `employee`
  `GET /api/employees/company/{companyId}`
  `GET /api/employees/{id}?companyId=...`
  `POST /api/employees`
- `position`
  `GET /api/positions?companyId=...&status=...`
  `GET /api/positions/{id}?companyId=...`
  `POST /api/positions`
  `PUT /api/positions/{id}?companyId=...`
  `DELETE /api/positions/{id}?companyId=...`
  `POST /api/positions/{id}/restore?companyId=...`
- `positiongroup`
  `GET /api/position-groups?companyId=...&status=...`
  `GET /api/position-groups/active?companyId=...`
  `GET /api/position-groups/non-active?companyId=...`
  `GET /api/position-groups/{id}?companyId=...`
  `POST /api/position-groups`
  `PUT /api/position-groups/{id}?companyId=...`
  `DELETE /api/position-groups/{id}?companyId=...`
- `category`
  `GET /api/categories?status=...`
  `GET /api/categories/{id}`
  `POST /api/categories`
  `PUT /api/categories/{id}`
  `DELETE /api/categories/{id}`
  `GET /api/category-groups?companyId=...&status=...`
  `GET /api/category-groups/{id}?companyId=...`
  `POST /api/category-groups`
  `PUT /api/category-groups/{id}?companyId=...`
  `DELETE /api/category-groups/{id}?companyId=...`
- `scheduling`
  `POST /api/scheduling/shifts`
  `GET /api/scheduling/shifts/employees?companyId=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd`
  `GET /api/scheduling/shifts/day-position?companyId=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd`
  `GET /api/scheduling/employees?companyId=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd` deprecated
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
- Category now relies on the authenticated tenant rather than request `companyId`.
- For request params, use the provided `companyId` where the contract still exposes it.
- For create endpoints, use the tenant id coming from the posted entity where applicable.

## Data and Query Notes
- `schema.sql` is a checked-in schema snapshot and should stay aligned with Flyway migrations.
- Scheduling grouped results are built from a custom SQL query plus service-level grouping logic.
- Grouped scheduling response uses day buckets relative to the requested `startDate`.
- Each day bucket carries the bucket date; individual shifts do not repeat that date.
- `availablePositions` in the grouped scheduling response is structured as `{ id, name }`.

## Testing
- Existing automated coverage is under `src/test/java/com/w2w/api/scheduling`.
- Run `mvn test` before pushing backend changes.
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
