package com.edwardjones.demo.alert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Day 2 connectivity test — verifies that Spring Boot can reach Databricks
 * via JDBC using the service principal credentials in your .env file.
 *
 * Run with:
 *   export $(cat .env | xargs) && ./gradlew test --tests "*DatabricksConnectivityTest"
 *
 * This test is skipped automatically if DATABRICKS_HOST is not set,
 * so it won't break CI pipelines that don't have Databricks credentials.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DATABRICKS_HOST", matches = ".+")
class DatabricksConnectivityTest {

    private static final Logger log = LoggerFactory.getLogger(DatabricksConnectivityTest.class);

    @Autowired
    @Qualifier("databricksDataSource")
    private DataSource databricksDataSource;

    @Autowired
    private AlertPollingService alertPollingService;

    @Test
    void databricks_select1_returns_true() throws Exception {
        log.info("Testing Databricks JDBC connectivity...");

        try (Connection conn = databricksDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1 AS ping")) {

            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("ping")).isEqualTo(1);
            log.info("✅ SELECT 1 succeeded — JDBC connection is working");
        }
    }

    @Test
    void hnw_alerts_table_is_readable() throws Exception {
        log.info("Testing hnw_alerts table read...");

        try (Connection conn = databricksDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) AS total FROM workspace.default.hnw_alerts")) {

            assertThat(rs.next()).isTrue();
            int total = rs.getInt("total");
            log.info("✅ hnw_alerts row count: {} (expected 10 after seeding)", total);
            assertThat(total).isGreaterThanOrEqualTo(10);
        }
    }

    @Test
    void alert_polling_service_testConnection_returns_true() {
        log.info("Testing AlertPollingService.testConnection()...");
        boolean result = alertPollingService.testConnection();
        assertThat(result)
            .as("AlertPollingService.testConnection() should return true when Databricks is reachable")
            .isTrue();
        log.info("✅ AlertPollingService connectivity confirmed");
    }
}
