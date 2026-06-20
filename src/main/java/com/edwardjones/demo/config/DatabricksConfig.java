package com.edwardjones.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.sql.Driver;

@Configuration
public class DatabricksConfig {

    @Value("${databricks.host}")
    private String host;

    @Value("${databricks.http-path}")
    private String httpPath;

    @Value("${databricks.client-id}")
    private String clientId;

    @Value("${databricks.client-secret}")
    private String clientSecret;

    @Value("${databricks.catalog:workspace}")
    private String catalog;

    @Value("${databricks.schema:default}")
    private String schema;

    /**
     * JDBC DataSource for Databricks SQL Warehouse.
     *
     * Auth method: OAuth M2M (Machine-to-Machine) using service principal.
     *   AuthMech=11  — Databricks OAuth
     *   Auth_Flow=1  — Client Credentials flow
     *
     * The Databricks JDBC driver JAR must be present in libs/DatabricksJDBC.jar
     * Download from: https://www.databricks.com/spark/jdbc-drivers-download
     */
    @Bean(name = "databricksDataSource")
    public DataSource databricksDataSource() {
        System.out.println("=== DATABRICKS CONFIG ===");
        System.out.println("Host:      " + host);
        System.out.println("HTTP Path: " + httpPath);
        System.out.println("Catalog:   " + catalog);
        System.out.println("Schema:    " + schema);
        System.out.println("Client ID: " + clientId);
        System.out.println("========================");
            String url = String.format(
            "jdbc:databricks://%s:443;" +
            "transportMode=http;" +
            "ssl=1;" +
            "httpPath=%s;" +
            "AuthMech=11;" +
            "Auth_Flow=1;" +
            "OAuth2ClientId=%s;" +
            "OAuth2Secret=%s;" +
            "ConnCatalog=%s;" +
            "ConnSchema=%s",
            host, httpPath, clientId, clientSecret, catalog, schema
        );

        SimpleDriverDataSource ds = new SimpleDriverDataSource();
        try {
            @SuppressWarnings("unchecked")
            Class<Driver> driverClass = (Class<Driver>) Class.forName(
                "com.databricks.client.jdbc.Driver"
            );
            ds.setDriverClass(driverClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "Databricks JDBC driver not found. " +
                "Download DatabricksJDBC.jar and place it in the libs/ directory. " +
                "See: https://www.databricks.com/spark/jdbc-drivers-download", e
            );
        }
        ds.setUrl(url);
        return ds;
    }

    // Expose catalog/schema as beans for use in SQL queries
    @Bean(name = "databricksCatalog")
    public String databricksCatalog() {
        return catalog;
    }

    @Bean(name = "databricksSchema")
    public String databricksSchema() {
        return schema;
    }
}
