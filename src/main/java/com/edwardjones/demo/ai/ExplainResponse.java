package com.edwardjones.demo.ai;

/**
 * Response body for POST /api/explain
 */
public record ExplainResponse(
        String explanation,
        String disclaimer
) {
}
