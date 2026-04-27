package com.w2w.api.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

@Configuration
public class TenantDatabaseConfig {

    static int resolveDatabaseTenantId() {
        Integer tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : -1;
    }

    static boolean isInternalSystemLookup(int tenantId) {
        return tenantId == 0;
    }

    @Bean
    @Primary
    @DependsOn("flyway")
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .type(TenantAwareDataSource.class)
                .build();
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(@Value("${spring.flyway.url:${spring.datasource.url}}") String flywayUrl,
                         @Value("${spring.flyway.user:${spring.datasource.username}}") String flywayUser,
                         @Value("${spring.flyway.password:${spring.datasource.password}}") String flywayPassword,
                         @Value("${spring.datasource.username}") String appDatabaseUser,
                         @Value("${spring.datasource.password}") String appDatabasePassword) {
        return Flyway.configure()
                .dataSource(flywayUrl, flywayUser, flywayPassword)
                .locations("classpath:db/migration")
                .defaultSchema("public")
                .baselineVersion("0")
                .baselineOnMigrate(true)
                .placeholders(Map.of(
                        "app.db.user", appDatabaseUser,
                        "app.db.password", appDatabasePassword,
                        "flyway.db.user", flywayUser
                ))
                .load();
    }

    private static final class TenantAwareDataSource extends HikariDataSource {
        @Override
        public Connection getConnection() throws SQLException {
            return configureTenant(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return configureTenant(super.getConnection(username, password));
        }

        private Connection configureTenant(Connection connection) throws SQLException {
            int tenantId = resolveDatabaseTenantId();
            boolean isSystemLookup = isInternalSystemLookup(tenantId);

            try (PreparedStatement sql = connection.prepareStatement("SELECT set_config('app.current_tenant', ?, false)")) {
                sql.setString(1, String.valueOf(tenantId));
                sql.execute();
            }
            try (PreparedStatement sql = connection.prepareStatement("SELECT set_config('app.internal_system_lookup', ?, false)")) {
                sql.setString(1, String.valueOf(isSystemLookup));
                sql.execute();
            }
            return connection;
        }
    }
}
