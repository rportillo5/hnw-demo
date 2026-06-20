package com.edwardjones.demo.ai;

import com.edwardjones.demo.tax.WhatIfResult;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response from POST /api/assistant/query.
 *
 * Always includes `intent` so the frontend knows which sub-component to render.
 * Other fields are nullable depending on the intent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssistantResponse(
        String intent,               // lowercase intent name
        String explanation,          // EXPLAIN, COMPARE, PORTFOLIO_SUMMARY, alert-driven
        String disclaimer,           // present when explanation is present
        WhatIfResult whatIfResult,   // WHATIF only
        String message               // UNSUPPORTED only
) {
    // ── Factory methods ────────────────────────────────────────────────────

    public static AssistantResponse explain(String explanation, String disclaimer) {
        return new AssistantResponse("explain", explanation, disclaimer, null, null);
    }

    public static AssistantResponse whatIf(String explanation, String disclaimer,
                                            WhatIfResult result) {
        return new AssistantResponse("whatif", explanation, disclaimer, result, null);
    }

    public static AssistantResponse portfolioSummary(String explanation, String disclaimer) {
        return new AssistantResponse("portfolio_summary", explanation, disclaimer, null, null);
    }

    public static AssistantResponse unsupported(String message) {
        return new AssistantResponse("unsupported", null, null, null, message);
    }
}
