---
name: w2w-api-backend
description: Implement and review backend changes in the W2W API repository. Use when working on Spring Boot features, controllers, services, repositories, security, tenant-aware behavior, API contract updates, Flyway-backed model changes, or backend tests in this repo.
---

# W2W API Backend

Use this skill to keep W2W API backend changes aligned with the repository's current patterns and required follow-through work.

## Start Here

Read `AGENTS.md` first. Treat it as the repo contract for layout, DTO naming, tenant rules, API documentation updates, and test expectations.

Inspect the touched feature package before editing. Keep each feature under `src/main/java/com/w2w/api/<feature>` and keep `Controller` and `Service` classes at the feature root. Create `dto/`, `model/`, and `repository/` subfolders only when there are multiple files of that type.

## Workflow

### 1. Classify the change

Decide which of these applies before editing:

- Feature logic only
- API contract change
- Tenanting or authentication change
- Authorization or permission change
- Data model or migration change
- Scheduling query or grouping change

Use that classification to decide which supporting files must move with the code change.

### 2. Preserve the feature layout and naming rules

Keep code inside the owning feature package.

Name top-level inbound payloads `*Request`.

Name top-level outbound payloads `*Response`.

Name reusable nested API DTOs by business meaning, not `*Dto`. Prefer names such as `*Summary`, `*Detail`, `*Reference`, `*Bucket`, or `*Item`.

Use `record` for DTOs unless framework binding or helper behavior makes a class necessary.

Keep repository and query-mapping types on `*Projection`.

## Tenant and Auth Model

Treat tenant scope as request-driven.

For authenticated requests, check whether tenant is already derived from auth context rather than an explicit controller parameter. The current JWT flow resolves the user, rejects accounts without a company, and sets `TenantContext` from the authenticated user in `src/main/java/com/w2w/api/login/JwtAuthFilter.java`.

When changing an existing endpoint, verify whether `companyId` still belongs in the public contract or whether the endpoint now relies on the authenticated tenant. Reconcile the implementation, `openapi.yaml`, Postman examples, and `AGENTS.md` in the same change. Do not leave contract drift behind.

If a controller still owns tenant selection for a feature, keep that behavior explicit and consistent. If tenant resolution has moved into auth flow for that feature, remove stale `companyId` assumptions from docs and examples.

## Authorization Pattern

Prefer feature-scoped policy services plus method security when a mutation depends on role or permission flags.

Use the current position flow as the reference pattern:

- Feature policy bean in `src/main/java/com/w2w/api/position/PositionPolicy.java`
- Method-level `@PreAuthorize` checks in `src/main/java/com/w2w/api/position/PositionService.java`
- Method security enabled in `src/main/java/com/w2w/api/config/SecurityConfig.java`

When permission depends on both user role and manager-specific flags, load the current user from `LoginRepository` and resolve feature permissions from `ManagerPermissionsRepository` instead of hard-coding role-only checks.

Keep authorization checks close to the business operation they guard. Avoid duplicating permission logic across controllers.

## Error Handling Pattern

Prefer repo exception types for domain-level failures that should map to stable HTTP responses.

Use the current exception baseline in `src/main/java/com/w2w/api/config/exception/` and `src/main/java/com/w2w/api/config/GlobalExceptionHandler.java`:

- `ResourceNotFoundException` for missing feature resources
- `ForbiddenOperationException` or `AccessDeniedException` for forbidden operations
- `IllegalArgumentException` or `ResponseStatusException` only when the failure is truly request-shape or parameter validation oriented

If you add a new exception type, wire it into the global handler in the same change.

## Service and Repository Conventions

Keep services tenant-aware. Resolve the tenant once near the operation and pass the tenant id into repository calls as needed.

Prefer small helper methods such as `requirePosition(...)` or `requireActivePosition(...)` for fetch-and-validate flows.

When implementing soft delete behavior, decide whether reads should include deleted rows, active rows only, or all rows. Keep repository methods and status filters aligned with that decision.

If you add restore behavior for a soft-deleted resource, update the service, controller, contract docs, and tests together.

## Contract Sync Rules

When request or response contracts change, update all of the following in the same change:

- `openapi.yaml`
- `postman/w2w-api.postman_collection.json`
- `AGENTS.md` API Surface section

If the change affects persisted schema or seed data, add a new Flyway migration under `src/main/resources/db/migration` and keep `schema.sql` aligned with the resulting schema.

Do not edit already-applied shared migrations. Add a new incremental migration instead.

## Testing Matrix

Run `mvn test` before finishing backend changes.

For feature logic changes, update or add controller and service tests in the affected feature package.

For authorization changes, add both permission-focused service tests and web/security tests. Use the current position changes as the reference:

- `src/test/java/com/w2w/api/position/PositionServiceAuthorizationTest.java`
- `src/test/java/com/w2w/api/position/PositionControllerTest.java`
- `src/test/java/com/w2w/api/login/JwtAuthFilterTest.java`
- `src/test/java/com/w2w/api/config/SecurityConfigTest.java`

For scheduling query or grouping changes, update both service tests and repository mapping tests. If query shape changes materially, also inspect integration coverage under `src/test/java/com/w2w/api/scheduling/integration`.

## Change Checklist

Before finishing, verify each applicable item:

- Feature layout still matches `AGENTS.md`
- Tenant handling is consistent with the endpoint's auth model
- Policy or permission checks are enforced in the service layer
- Domain exceptions map cleanly through the global handler
- `openapi.yaml`, Postman, and `AGENTS.md` match the implementation
- Flyway and `schema.sql` are in sync when the model changes
- Tests cover logic, auth, and edge cases introduced by the change

## Good Triggers

Use this skill for requests such as:

- "Add a new backend endpoint in this repo"
- "Update a W2W service and keep OpenAPI and Postman in sync"
- "Implement manager-permission checks for a feature"
- "Refactor a tenant-aware controller or service"
- "Add a Flyway-backed model change and the corresponding tests"
