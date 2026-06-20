package com.edwardjones.demo.api;

import com.edwardjones.demo.ai.*;
import com.edwardjones.demo.domain.TaxLot;
import com.edwardjones.demo.domain.TaxLotRepository;
import com.edwardjones.demo.tax.WhatIfResult;
import com.edwardjones.demo.tax.WhatIfTaxCalculator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * POST /api/assistant/query
 *
 * The single AI-powered entry point for advisor free-text queries.
 * Flow:
 *   1. IntentRouter classifies the query (one LLM call, returns one word)
 *   2. Dispatch to the appropriate deterministic service
 *   3. TaxExplanationService generates plain-English explanation (second LLM call)
 *   4. Return AssistantResponse with intent field + relevant data
 */
@RestController
@RequestMapping("/api/assistant")
@CrossOrigin(origins = "*")
public class AssistantController {

    private static final Logger log = LoggerFactory.getLogger(AssistantController.class);

    private static final String UNSUPPORTED_MESSAGE =
        "I can explain tax lot details, run what-if calculations, or summarize " +
        "the portfolio. For investment recommendations or market predictions, " +
        "please use your firm's research tools or consult a supervisor.";

    private final IntentRouter intentRouter;
    private final TaxExplanationService explanationService;
    private final WhatIfTaxCalculator whatIfCalculator;
    private final TaxLotRepository taxLotRepository;

    public AssistantController(IntentRouter intentRouter,
                                TaxExplanationService explanationService,
                                WhatIfTaxCalculator whatIfCalculator,
                                TaxLotRepository taxLotRepository) {
        this.intentRouter = intentRouter;
        this.explanationService = explanationService;
        this.whatIfCalculator = whatIfCalculator;
        this.taxLotRepository = taxLotRepository;
    }

    @PostMapping("/query")
    public ResponseEntity<?> query(@Valid @RequestBody AssistantRequest request) {
        log.info("Assistant query: '{}' (lotId: {}, alertContext: {})",
            request.text(), request.currentLotId(),
            request.alertContext() != null ? request.alertContext().alertType() : "none");

        // ── Step 1: classify intent ───────────────────────────────────────
        Intent intent = intentRouter.classify(request.text());

        // ── Step 2: dispatch ──────────────────────────────────────────────
        try {
            AssistantResponse response = switch (intent) {

                case EXPLAIN -> handleExplain(request);

                case WHATIF -> handleWhatIf(request);

                case PORTFOLIO_SUMMARY -> handlePortfolioSummary(request);

                case COMPARE -> handleCompare(request);

                case UNSUPPORTED -> AssistantResponse.unsupported(UNSUPPORTED_MESSAGE);
            };

            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest()
                .body("Invalid lot ID or portfolio ID: " + e.getMessage());
        }
    }

    // ── Intent handlers ───────────────────────────────────────────────────────

    private AssistantResponse handleExplain(AssistantRequest request) {
        // Alert-context path: "Ask AI" button on Databricks AlertCard
        if (request.alertContext() != null && request.currentLotId() == null) {
            AssistantRequest.AlertContext ctx = request.alertContext();
            String alertPrompt = buildAlertExplainPrompt(ctx, request.text());
            ExplainResponse resp = explanationService.explainFreeText(alertPrompt);
            return AssistantResponse.explain(resp.explanation(), resp.disclaimer());
        }

        // Standard path: lot selected from harvest table
        if (request.currentLotId() == null) {
            return AssistantResponse.unsupported(
                "Please select a tax lot from the harvest opportunities list first.");
        }
        ExplainResponse resp = explanationService.explain(request.currentLotId(), request.text());
        return AssistantResponse.explain(resp.explanation(), resp.disclaimer());
    }

    private AssistantResponse handleWhatIf(AssistantRequest request) {
        if (request.currentLotId() == null) {
            return AssistantResponse.unsupported(
                "To run a what-if, please select a tax lot first and specify " +
                "a replacement ticker (e.g. 'what if I sell this and buy VTI?').");
        }

        // Extract replacement ticker from the query text
        String replacementTicker = extractReplacementTicker(request.text());
        if (replacementTicker == null) {
            return AssistantResponse.unsupported(
                "I can run a what-if calculation — please mention the replacement " +
                "ticker in your query (e.g. 'what if I sell this and buy VTI?').");
        }

        WhatIfResult result = whatIfCalculator.calculate(request.currentLotId(), replacementTicker);

        // Generate a plain-English explanation of the what-if result
        ExplainResponse explanation = explanationService.explain(request.currentLotId(), "whatif");
        return AssistantResponse.whatIf(explanation.explanation(), explanation.disclaimer(), result);
    }

    private AssistantResponse handlePortfolioSummary(AssistantRequest request) {
        String summaryPrompt = String.format(
            "Give a brief portfolio overview summary for an advisor preparing " +
            "to meet with their HNW client. The portfolio has harvesting " +
            "opportunities in TSLA and INTC, and the fixed income allocation " +
            "has drifted below target. Keep it to 3-4 sentences."
        );
        ExplainResponse resp = explanationService.explainFreeText(summaryPrompt);
        return AssistantResponse.portfolioSummary(resp.explanation(), resp.disclaimer());
    }

    private AssistantResponse handleCompare(AssistantRequest request) {
        // Simplified — treat compare as explain with a compare context hint
        if (request.currentLotId() == null) {
            return AssistantResponse.unsupported(
                "To compare lots, please select a lot from the harvest list first.");
        }
        ExplainResponse resp = explanationService.explain(request.currentLotId(), "compare");
        return AssistantResponse.explain(resp.explanation(), resp.disclaimer());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Very simple ticker extraction — looks for known tickers or common ETFs
     * mentioned after "buy" or "into" in the query text.
     * In a production system this would be a proper NER step.
     */
    private String extractReplacementTicker(String text) {
        String upper = text.toUpperCase();
        // Common replacement tickers from demo seed data
        for (String ticker : new String[]{"VTI", "ITOT", "SCHB", "BND", "AGG",
                                           "SCHZ", "AAPL", "MSFT", "AMZN"}) {
            if (upper.contains(ticker)) return ticker;
        }
        return null;
    }

    private String buildAlertExplainPrompt(AssistantRequest.AlertContext ctx, String userText) {
        return String.format(
            "Explain this portfolio alert to an advisor preparing for a client conversation.\n\n" +
            "Alert type: %s\n" +
            "Ticker: %s\n" +
            "Alert detail: %s\n\n" +
            "Advisor question: %s",
            ctx.alertType(),
            ctx.ticker() != null ? ctx.ticker() : "N/A",
            ctx.detail(),
            userText
        );
    }
}
