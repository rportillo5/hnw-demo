package com.edwardjones.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AnthropicConfig {

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${anthropic.model:claude-sonnet-4-6}")
    private String model;

    @Value("${anthropic.max-tokens:1024}")
    private int maxTokens;

    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public int getMaxTokens() { return maxTokens; }

    /**
     * Shared HTTP client for all Anthropic API calls.
     * java.net.http.HttpClient is built into Java 21 — no extra dependency needed.
     */
    @Bean(name = "anthropicHttpClient")
    public HttpClient anthropicHttpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
}
