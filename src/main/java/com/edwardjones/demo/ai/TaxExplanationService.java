package com.edwardjones.demo.ai;

import com.edwardjones.demo.config.AnthropicConfig;
import com.edwardjones.demo.domain.TaxLot;
import com.edwardjones.demo.domain.TaxLotRepository;
import com.edwardjones.demo.tax.TaxClassification;
import com.edwardjones.demo.tax.TaxRulesEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Generates plain-English explanations of tax-loss harvest opportunities
 * for advisor-client conversations.
 *
 * IMPORTANT: this service NEVER performs financial calculations. All numbers
 * (gain/loss, tax savings) are computed by TaxRulesEngine beforehand and
 * passed into the prompt as already-known facts. The LLM's job is purely
 * to explain those facts in plain, audience-friendly English.
 *
 * Updated (Day 8 polish): system prompt revised to produce audience-friendly
 * responses — jargon-free, dollar-outcome-first, with a verbatim advisor
 * talking point at the end of every explanation.
 */
@Service
public class TaxExplanationService {

    private static final Logger log = LoggerFactory.getLogger(TaxExplanationService.class);

    static final String DISCLAIMER =
        "This explanation is for informational purposes only and does not constitute " +
        "tax or legal advice. Please consult a qualified tax professional.";

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        You are a tax-aware financial advisor assistant helping Edward Jones advisors
        explain portfolio recommendations to a live audience that includes both
        financial professionals and non-financial observers.

        RULES:
        1. Lead with the DOLLAR OUTCOME first — what does this actually mean for
           the client's wallet? State the dollar amount in the very first sentence.
        2. Explain the mechanism in plain everyday language. Avoid all financial
           jargon. If you must use a technical term, immediately follow it with
           a plain-English parenthetical explanation in parentheses.
           Examples:
             ❌ "The wash-sale rule prohibits substantially identical repurchases"
             ✅ "The IRS has a 30-day restriction (called the wash-sale rule) that
                cancels the tax benefit if you buy the same stock back too soon"
             ❌ "Long-term capital gains rate of 23.8%%"
             ✅ "a lower tax rate of about 24%% — because the investment was held
                for more than a year"
        3. Use **bold** to highlight the single most important number or phrase
           in your explanation. Maximum 2 bold phrases per response.
        4. Keep explanations to 3-4 sentences — short enough for an audience
           to follow in real time.
        5. End every explanation with exactly one sentence an advisor could say
           verbatim to their client in a meeting. Format it as:
           "You might tell your client: [sentence]"
        6. Never recommend a specific action. Frame as "one option advisors
           sometimes consider is..."
        7. Always include this exact disclaimer as the very last line:
           "%s"

        BACKGROUND KNOWLEDGE (use to inform your explanation, do not quote verbatim):
        %s
        """;

    private final AnthropicClient anthropicClient;
    private final AnthropicConfig anthropicConfig;
    private final RagKnowledgeBase knowledgeBase;
    private final TaxLotRepository taxLotRepository;
    private final TaxRulesEngine taxRulesEngine;
    private final ExplainAuditLogRepository auditLogRepository;

    public TaxExplanationService(
            AnthropicClient anthropicClient,
            AnthropicConfig anthropicConfig,
            RagKnowledgeBase knowledgeBase,
            TaxLotRepository taxLotRepository,
            TaxRulesEngine taxRulesEngine,
            ExplainAuditLogRepository auditLogRepository) {
        this.anthropicClient = anthropicClient;
        this.anthropicConfig = anthropicConfig;
        this.knowledgeBase = knowledgeBase;
        this.taxLotRepository = taxLotRepository;
        this.taxRulesEngine = taxRulesEngine;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Generate a plain-English explanation for the given tax lot.
     *
     * @param lotId   the lot to explain
     * @param context optional hint — "harvest", "whatif", "compare"
     * @throws NoSuchElementException if lotId doesn't match any lot
     */
    public ExplainResponse explain(UUID lotId, String context) {
        TaxLot lot = taxLotRepository.findById(lotId)
            .orElseThrow(() -> new NoSuchElementException("No tax lot found with id: " + lotId));

        TaxClassification classification = taxRulesEngine.classify(lot);

        String userMessage = buildUserMessage(lot, classification, context);
        String systemPrompt = buildSystemPrompt();

        String rawResponse = anthropicClient.complete(systemPrompt, userMessage);
        String finalResponse = ensureDisclaimerPresent(rawResponse);

        auditLog(lotId, systemPrompt + userMessage, finalResponse);

        return new ExplainResponse(finalResponse, DISCLAIMER);
    }

    /**
     * Free-text explanation path — used for:
     *   - Alert-context queries ("Ask AI" on a Databricks AlertCard)
     *   - Portfolio summary queries
     *
     * Unlike explain(lotId, context), this method receives the full prompt
     * text directly from the AssistantController — no lot lookup needed.
     * Disclaimer enforcement and audit logging still apply.
     */
    public ExplainResponse explainFreeText(String userMessage) {
        String systemPrompt = buildSystemPrompt();
        String rawResponse = anthropicClient.complete(systemPrompt, userMessage);
        String finalResponse = ensureDisclaimerPresent(rawResponse);

        // Audit log with a synthetic lot ID for non-lot queries
        auditLog(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                 systemPrompt + userMessage, finalResponse);

        return new ExplainResponse(finalResponse, DISCLAIMER);
    }

    // ── Streaming variants ────────────────────────────────────────────────────

    /**
     * Streaming variant of explain(). Pushes text chunks to onChunk as they
     * arrive, then returns the full assembled response for audit logging.
     */
    public ExplainResponse streamExplain(UUID lotId, String context, Consumer<String> onChunk) {
        TaxLot lot = taxLotRepository.findById(lotId)
            .orElseThrow(() -> new NoSuchElementException("No tax lot found with id: " + lotId));

        TaxClassification classification = taxRulesEngine.classify(lot);
        String userMessage = buildUserMessage(lot, classification, context);
        String systemPrompt = buildSystemPrompt();

        StringBuilder fullText = new StringBuilder();
        anthropicClient.streamComplete(systemPrompt, userMessage, chunk -> {
            fullText.append(chunk);
            onChunk.accept(chunk);
        });

        String finalResponse = ensureDisclaimerPresent(fullText.toString());
        if (finalResponse.length() > fullText.length()) {
            onChunk.accept(finalResponse.substring(fullText.length()));
        }

        auditLog(lotId, systemPrompt + userMessage, finalResponse);
        return new ExplainResponse(finalResponse, DISCLAIMER);
    }

    /**
     * Streaming variant of explainFreeText().
     */
    public ExplainResponse streamExplainFreeText(String userMessage, Consumer<String> onChunk) {
        String systemPrompt = buildSystemPrompt();

        StringBuilder fullText = new StringBuilder();
        anthropicClient.streamComplete(systemPrompt, userMessage, chunk -> {
            fullText.append(chunk);
            onChunk.accept(chunk);
        });

        String finalResponse = ensureDisclaimerPresent(fullText.toString());
        if (finalResponse.length() > fullText.length()) {
            onChunk.accept(finalResponse.substring(fullText.length()));
        }

        auditLog(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                 systemPrompt + userMessage, finalResponse);
        return new ExplainResponse(finalResponse, DISCLAIMER);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildSystemPrompt() {
        return SYSTEM_PROMPT_TEMPLATE.formatted(DISCLAIMER, knowledgeBase.asFormattedContext());
    }

    private String buildUserMessage(TaxLot lot, TaxClassification classification, String context) {
        var savings = taxRulesEngine.estimatedTaxSavings(classification);

        String contextHint = context != null ? switch (context) {
            case "whatif"  -> "\nContext: The advisor is considering selling this lot and buying a replacement.";
            case "compare" -> "\nContext: The advisor wants to compare this lot with another.";
            case "harvest" -> "\nContext: The advisor is evaluating this as a tax-loss harvest candidate.";
            default        -> "";
        } : "";

        return """
            Explain this tax lot situation to an advisor preparing for a client conversation.
            Remember: lead with the dollar outcome, use plain language, bold the most
            important figure, and end with a verbatim client-facing talking point.
            %s

            Ticker: %s
            Lot: %s
            Purchase date: %s
            Days held: %d (%s)
            Shares: %s
            Cost basis per share: $%s
            Current price per share: $%s
            Unrealized gain/loss: $%s
            Estimated tax savings if harvested: $%s
            """.formatted(
                contextHint,
                lot.getTicker(),
                lot.getLotLabel(),
                lot.getPurchaseDate(),
                classification.daysHeld(),
                classification.period() == com.edwardjones.demo.tax.HoldingPeriod.LONG_TERM
                    ? "held over 1 year — lower tax rate applies"
                    : "held under 1 year — higher tax rate applies",
                lot.getShares(),
                lot.getCostBasisPerShare(),
                lot.getCurrentPrice(),
                classification.gainLoss(),
                savings
            );
    }

    /**
     * Compliance safeguard: scan the LLM response for the exact disclaimer
     * text. If absent (the model occasionally drops it), append it.
     * This guarantees the disclaimer is present regardless of model behavior.
     */
    String ensureDisclaimerPresent(String response) {
        if (response.contains(DISCLAIMER)) {
            return response;
        }
        log.warn("LLM response omitted required disclaimer — appending server-side");
        return response.trim() + "\n\n" + DISCLAIMER;
    }

    private void auditLog(UUID lotId, String fullPrompt, String responseText) {
        String promptHash = sha256(fullPrompt);
        ExplainAuditLog entry = new ExplainAuditLog(
            lotId, promptHash, responseText, anthropicConfig.getModel(), Instant.now()
        );
        auditLogRepository.save(entry);
        log.info("Explanation audit logged for lot {} (hash: {})",
            lotId, promptHash.substring(0, 8));
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
