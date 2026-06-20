package com.edwardjones.demo.ai;

import com.edwardjones.demo.config.AnthropicConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Thin wrapper around the Anthropic /v1/messages endpoint.
 * Used by TaxExplanationService and (later, Day 8) IntentRouter.
 *
 * Deliberately minimal — no streaming, no tool use, no conversation history.
 * Each call is a single-turn request: system prompt + one user message in,
 * text response out.
 */
@Component
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient httpClient;
    private final AnthropicConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnthropicClient(@Qualifier("anthropicHttpClient") HttpClient httpClient,
                            AnthropicConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    /**
     * Single-turn completion call.
     *
     * @param systemPrompt the system prompt (instructions, persona, rules)
     * @param userMessage  the user-turn content
     * @return the concatenated text content of the response
     * @throws AnthropicApiException if the call fails or returns a non-2xx status
     */
    public String complete(String systemPrompt, String userMessage) {
        try {
            String requestBody = buildRequestBody(systemPrompt, userMessage);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Anthropic API returned status {}: {}", response.statusCode(), response.body());
                throw new AnthropicApiException(
                    "Anthropic API call failed with status " + response.statusCode() + ": " + response.body());
            }

            return extractTextContent(response.body());

        } catch (java.io.IOException | InterruptedException e) {
            log.error("Anthropic API call failed: {}", e.getMessage());
            throw new AnthropicApiException("Anthropic API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Streaming completion — calls Anthropic with stream=true and invokes
     * onChunk for each text delta as it arrives. Blocks until the stream ends.
     */
    public void streamComplete(String systemPrompt, String userMessage, Consumer<String> onChunk) {
        try {
            String requestBody = buildRequestBody(systemPrompt, userMessage, true);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<java.util.stream.Stream<String>> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                String errorBody = response.body().collect(Collectors.joining("\n"));
                throw new AnthropicApiException(
                    "Anthropic streaming API failed with status " + response.statusCode() + ": " + errorBody);
            }

            response.body().forEach(line -> {
                if (!line.startsWith("data: ")) return;
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data) || data.isEmpty()) return;
                try {
                    JsonNode node = objectMapper.readTree(data);
                    if ("content_block_delta".equals(node.path("type").asText())) {
                        JsonNode delta = node.path("delta");
                        if ("text_delta".equals(delta.path("type").asText())) {
                            String text = delta.path("text").asText();
                            if (!text.isEmpty()) onChunk.accept(text);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Skipping unparseable SSE line: {}", data);
                }
            });

        } catch (java.io.IOException | InterruptedException e) {
            log.error("Anthropic streaming call failed: {}", e.getMessage());
            throw new AnthropicApiException("Anthropic streaming call failed: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String systemPrompt, String userMessage) {
        return buildRequestBody(systemPrompt, userMessage, false);
    }

    private String buildRequestBody(String systemPrompt, String userMessage, boolean stream) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", config.getModel());
        root.put("max_tokens", config.getMaxTokens());
        if (stream) root.put("stream", true);
        root.put("system", systemPrompt);

        ArrayNode messages = root.putArray("messages");
        ObjectNode userTurn = messages.addObject();
        userTurn.put("role", "user");
        userTurn.put("content", userMessage);

        return root.toString();
    }

    private String extractTextContent(String responseBody) throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentArray = root.get("content");

        if (contentArray == null || !contentArray.isArray()) {
            throw new AnthropicApiException("Unexpected response shape — no content array: " + responseBody);
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode block : contentArray) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }

        if (text.isEmpty()) {
            throw new AnthropicApiException("No text content found in response: " + responseBody);
        }

        return text.toString();
    }

    public static class AnthropicApiException extends RuntimeException {
        public AnthropicApiException(String message) {
            super(message);
        }
        public AnthropicApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
