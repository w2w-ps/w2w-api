package com.w2w.api.scheduling.integration;

import com.w2w.api.config.TenantContext;
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

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("w2w_test")
            .withUsername("w2w_test")
            .withPassword("w2w_test");

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.jdbc-url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void resetTestData() {
        TenantContext.setCurrentTenant(0);
        jdbcTemplate.update("DELETE FROM scheduled_employee WHERE company_id BETWEEN ? AND ?", TEST_COMPANY_MIN, TEST_COMPANY_MAX);
        jdbcTemplate.update("""
                DELETE FROM employee_skill
                WHERE employee_id IN (
                    SELECT employee_id
                    FROM employee
                    WHERE company_id BETWEEN ? AND ?
                )
                """, TEST_COMPANY_MIN, TEST_COMPANY_MAX);
        jdbcTemplate.update("""
                DELETE FROM employee_phone
                WHERE employee_id IN (
                    SELECT employee_id
                    FROM employee
                    WHERE company_id BETWEEN ? AND ?
                )
                """, TEST_COMPANY_MIN, TEST_COMPANY_MAX);
        jdbcTemplate.update("DELETE FROM schedule WHERE company_id BETWEEN ? AND ?", TEST_COMPANY_MIN, TEST_COMPANY_MAX);
        jdbcTemplate.update("DELETE FROM employee WHERE company_id BETWEEN ? AND ?", TEST_COMPANY_MIN, TEST_COMPANY_MAX);
        jdbcTemplate.update("DELETE FROM category WHERE company_id BETWEEN ? AND ?", TEST_COMPANY_MIN, TEST_COMPANY_MAX);
        jdbcTemplate.update("DELETE FROM skill WHERE company_id BETWEEN ? AND ?", TEST_COMPANY_MIN, TEST_COMPANY_MAX);
        jdbcTemplate.update("DELETE FROM company WHERE company_id BETWEEN ? AND ?", TEST_COMPANY_MIN, TEST_COMPANY_MAX);
        TenantContext.clear();
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }
}
