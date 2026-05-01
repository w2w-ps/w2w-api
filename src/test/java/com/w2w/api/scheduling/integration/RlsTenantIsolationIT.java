package com.w2w.api.scheduling.integration;

import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.repository.EmployeeRepository;
import com.w2w.api.tenant.Company;
import com.w2w.api.tenant.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that PostgreSQL Row Level Security enforces tenant isolation at the
 * database layer, independent of any application-level WHERE clauses.
 *
 * Uses company IDs 7021/7022 to avoid collisions with SchedulingServiceIT
 * (7001/7002) and SchedulingQueryRepositoryIT (7011/7012). The base class
 * resetTestData() cleans the full 7000–7999 range before each test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class RlsTenantIsolationIT extends PostgresIntegrationTestBase {

    private static final int COMPANY_A_ID = 7021;
    private static final int COMPANY_B_ID = 7022;
    private static final int EMPLOYEE_A_ID = 702101;
    private static final int EMPLOYEE_B_ID = 702201;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Proves DB-enforced cross-tenant row isolation.
     * employeeRepository.findAll() issues a SELECT with no WHERE clause;
     * RLS filters rows before they reach the application layer.
     */
    @Test
    void rlsPolicy_preventsCompanyBFromReadingCompanyARows() {
        createCompany(COMPANY_A_ID, "RLS Tenant A");
        createCompany(COMPANY_B_ID, "RLS Tenant B");

        TenantContext.setCurrentTenant(COMPANY_A_ID);
        createEmployee(EMPLOYEE_A_ID, COMPANY_A_ID, "Alice", "Rls");

        TenantContext.setCurrentTenant(COMPANY_B_ID);
        createEmployee(EMPLOYEE_B_ID, COMPANY_B_ID, "Bob", "Rls");

        // findAll() with no WHERE clause: RLS must filter to current tenant only
        TenantContext.setCurrentTenant(COMPANY_B_ID);
        List<Integer> visibleIds = employeeRepository.findAll().stream()
                .map(Employee::getEmployeeId)
                .toList();
        assertFalse(visibleIds.contains(EMPLOYEE_A_ID),
                "RLS must hide company A's employee from company B tenant");
        assertTrue(visibleIds.contains(EMPLOYEE_B_ID),
                "Company B tenant must see its own employee");

        // Reciprocal: company A sees only its own data
        TenantContext.setCurrentTenant(COMPANY_A_ID);
        List<Integer> companyAIds = employeeRepository.findAll().stream()
                .map(Employee::getEmployeeId)
                .toList();
        assertTrue(companyAIds.contains(EMPLOYEE_A_ID));
        assertFalse(companyAIds.contains(EMPLOYEE_B_ID),
                "RLS must hide company B's employee from company A tenant");
    }

    /**
     * Verifies that V2 seed data was inserted under the correct tenant context
     * and is accessible only when the matching tenant context is active.
     * Also confirms that global tables (emp_type, user_roles) are always
     * readable regardless of tenant context.
     */
    @Test
    void seedData_tenantRowsIsolatedAndGlobalTablesAlwaysReadable() {
        // Global tables have no RLS — must be readable from any tenant context
        TenantContext.setCurrentTenant(COMPANY_B_ID);
        int empTypeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM emp_type", Integer.class);
        assertEquals(5, empTypeCount,
                "emp_type (global, no RLS) must return all rows from any tenant context");

        int roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles", Integer.class);
        assertEquals(3, roleCount,
                "user_roles (global, no RLS) must return all rows from any tenant context");

        // Seeded company 1 employees must be visible only when tenant context = 1
        TenantContext.setCurrentTenant(1);
        int countAsCompany1 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE company_id = 1", Integer.class);
        assertTrue(countAsCompany1 > 0,
                "Company 1 employees must be visible when tenant context = 1");

        // Same query under a different tenant context must return 0 (RLS blocks it)
        TenantContext.setCurrentTenant(COMPANY_B_ID);
        int countAsCompanyB = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE company_id = 1", Integer.class);
        assertEquals(0, countAsCompanyB,
                "Company 1 employees must not be visible when tenant context = company B");
    }

    private void createCompany(int companyId, String name) {
        TenantContext.setCurrentTenant(companyId);
        if (companyRepository.existsById(companyId)) {
            return;
        }
        Company company = new Company();
        company.setCompanyId(companyId);
        company.setCompanyName(name);
        company.setDepartmentName("RLS Test");
        company.setStatus("active");
        company.setTimestamp(LocalDateTime.of(2026, 6, 1, 0, 0));
        companyRepository.save(company);
    }

    private void createEmployee(int employeeId, int companyId, String firstName, String lastName) {
        jdbcTemplate.update(
                """
                INSERT INTO employee
                    (employee_id, company_id, status, first_name, last_name, email, hire_date)
                VALUES (?, ?, 'active', ?, ?, ?, ?)
                """,
                employeeId, companyId, firstName, lastName,
                firstName.toLowerCase() + "." + lastName.toLowerCase() + "@rls-test.com",
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
