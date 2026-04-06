package com.w2w.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class TenantDatabaseConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .type(TenantAwareDataSource.class)
                .build();
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .defaultSchema("public")
                .baselineVersion("0")
                .baselineOnMigrate(true)
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
            try (Statement sql = connection.createStatement()) {
                sql.execute("SET app.current_tenant = '" + resolveTenantId() + "'");
            }
            return connection;
        }

        private int resolveTenantId() {
            Integer tenantId = TenantContext.getCurrentTenant();
            return tenantId != null ? tenantId : 0;
        }
    }
}
