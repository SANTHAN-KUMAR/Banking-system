package com.santhan.banking_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Overrides the default spring.datasource configuration to
 * support Railway’s mysql://user:pass@host:port/db format.
 */
@Configuration
public class DatabaseConfig {

    /**
     * Railway injects a URL like mysql://user:pass@host:3306/db
     * into the MYSQL_URL env var.  We re‐write it to jdbc:mysql://...
     */
    @Value("${MYSQL_URL:#{null}}")
    private String mysqlUrl;

    /** Fallback creds if user/pass are not embedded in the URL */
    @Value("${SPRING_DATASOURCE_USERNAME:root}")
    private String username;

    @Value("${SPRING_DATASOURCE_PASSWORD:}")
    private String password;

    @Value("${SPRING_DATASOURCE_DRIVER_CLASS_NAME:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;

    @Primary
    @Bean
    public DataSource dataSource() {
        DataSourceBuilder<?> ds = DataSourceBuilder.create()
            .driverClassName(driverClassName);

        if (mysqlUrl != null && mysqlUrl.startsWith("mysql://")) {
            // Rewrite the Railway URL to proper JDBC form
            String jdbcUrl = convertToJdbcUrl(mysqlUrl);
            ds.url(jdbcUrl);

            // If credentials are embedded, pull them out; otherwise use fallbacks
            String userInfo = mysqlUrl.substring(8, mysqlUrl.indexOf('@'));
            if (userInfo.contains(":")) {
                ds.username(userInfo.substring(0, userInfo.indexOf(':')));
                ds.password(userInfo.substring(userInfo.indexOf(':') + 1));
            } else {
                ds.username(username).password(password);
            }
        } else {
            // No MYSQL_URL provided, fall back to standard properties
            ds.url("jdbc:mysql://localhost:3306/banking_system")
              .username(username)
              .password(password);
        }

        return ds.build();
    }

    private String convertToJdbcUrl(String url) {
        // Strip off “mysql://user:pass@” → “jdbc:mysql://host:port/db”
        int at = url.indexOf('@');
        return (at > 0
            ? "jdbc:mysql://" + url.substring(at + 1)
            : "jdbc:" + url);
    }
}
