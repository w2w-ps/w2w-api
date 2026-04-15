package com.w2w.api.scheduling.integration;

import com.w2w.api.category.repository.CategoryRepository;
import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.repository.EmployeeRepository;
import com.w2w.api.login.LoginRepository;
import com.w2w.api.position.repository.PositionRepository;
import com.w2w.api.scheduling.ScheduleRepository;
import com.w2w.api.scheduling.ShiftRepository;
import com.w2w.api.tenant.CompanyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class PostgresIntegrationTestBase {
    protected static final int TEST_COMPANY_MIN = 7000;
    protected static final int TEST_COMPANY_MAX = 7999;

    private static final String APP_USER = "w2w_app_test";
    private static final String APP_PASSWORD = "w2w_app_test";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("w2w_test")
            .withUsername("w2w_test")
            .withPassword("w2w_test");

    @Autowired
    protected LoginRepository loginRepository;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ShiftRepository shiftRepository;

    @Autowired
    protected EmployeeRepository employeeRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected PositionRepository positionRepository;

    @Autowired
    protected ScheduleRepository scheduleRepository;

    @Autowired
    protected CompanyRepository companyRepository;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        // App datasource: non-superuser, subject to RLS.
        // The consolidated base schema provisions this role when it differs from the Flyway user.
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.jdbc-url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> APP_USER);
        registry.add("spring.datasource.password", () -> APP_PASSWORD);

        // Flyway: runs as the container superuser (table owner, bypasses RLS during migrations).
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void resetTestData() {
        TenantContext.setCurrentTenant(0);
        loginRepository.deleteAll(
                loginRepository.findAll().stream()
                        .filter(user -> isTestCompany(user.getCompanyId()))
                        .toList()
        );
        shiftRepository.deleteAll(
                shiftRepository.findAll().stream()
                        .filter(shift -> isTestCompany(shift.getCompanyId()))
                        .toList()
        );

        categoryRepository.deleteAll(
                categoryRepository.findAll().stream()
                        .filter(category -> isTestCompany(category.getCompanyId()))
                        .toList()
        );
        positionRepository.deleteAll(
                positionRepository.findAll().stream()
                        .filter(position -> isTestCompany(position.getCompanyId()))
                        .toList()
        );
        scheduleRepository.deleteAll(
                scheduleRepository.findAll().stream()
                        .filter(schedule -> isTestCompany(schedule.getCompanyId()))
                        .toList()
        );
        companyRepository.deleteAll(
                companyRepository.findAll().stream()
                        .filter(company -> isTestCompany(company.getCompanyId()))
                        .toList()
        );
        TenantContext.clear();
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    private boolean isTestCompany(Integer companyId) {
        return companyId != null && companyId >= TEST_COMPANY_MIN && companyId <= TEST_COMPANY_MAX;
    }
}
