package com.edwardjones.demo.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Classifies free-text advisor queries into one of five intents using
 * a single Claude API call. The LLM returns only a single word —
 * intent extraction, not generation.
 *
 * Per 05-ai-explanation-service.md: the router SELECTS and PARAMETERIZES
 * deterministic method calls. It does not perform calculations or give advice.
 */
@Component
public class IntentRouter {

    private static final Logger log = LoggerFactory.getLogger(IntentRouter.class);

    private static final String CLASSIFICATION_PROMPT = """
        Classify the following advisor query into exactly one of these intents:
        explain, whatif, compare, portfolio_summary, unsupported

        Rules:
        - Return ONLY the intent word, lowercase, nothing else.
        - "unsupported" means the query asks for a recommendation, prediction,
          or opinion that goes beyond explaining a computed result.
        - When ambiguous between explain and whatif, prefer whatif if the query
          contains "if", "would", "what happens", or a conditional verb.
        - "portfolio_summary" means the advisor wants an overview of the whole
          portfolio (e.g. "summarize", "overview", "how is she doing").

        Query: "%s"

        Intent:""";

    private final AnthropicClient anthropicClient;

    public IntentRouter(AnthropicClient anthropicClient) {
        this.anthropicClient = anthropicClient;
    }

    /**
     * Classify the given free-text query into an Intent.
     * Falls back to UNSUPPORTED if the model returns an unrecognised value.
     */
    public Intent classify(String userText) {
        String prompt = CLASSIFICATION_PROMPT.formatted(userText);
        String raw = anthropicClient.complete("You are a precise intent classifier.", prompt);
        String cleaned = raw.strip().toLowerCase().replaceAll("[^a-z_]", "");

        Intent intent = switch (cleaned) {
            case "explain"           -> Intent.EXPLAIN;
            case "whatif"            -> Intent.WHATIF;
            case "compare"           -> Intent.COMPARE;
            case "portfolio_summary" -> Intent.PORTFOLIO_SUMMARY;
            default                  -> Intent.UNSUPPORTED;
        };

        log.info("Intent classified: '{}' → {} (raw: '{}')", userText, intent, raw.strip());
        return intent;
    }
}
