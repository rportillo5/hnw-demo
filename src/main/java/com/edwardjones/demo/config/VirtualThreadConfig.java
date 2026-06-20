package com.edwardjones.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadConfig {

    /**
     * Virtual-thread-per-task executor (Java 21 Project Loom).
     *
     * Used by:
     *   - AlertPollingService  (Databricks JDBC poll every 10s)
     *   - PortfolioScanService (concurrent lot price fetches)
     *   - TaxExplanationService (Claude API calls)
     *
     * Demo talking point: "Each I/O-bound task gets its own virtual thread.
     * We can run hundreds of concurrent advisor sessions with the same
     * resource footprint as a handful of platform threads."
     */
    @Bean(name = "virtualThreadExecutor", destroyMethod = "shutdown")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
