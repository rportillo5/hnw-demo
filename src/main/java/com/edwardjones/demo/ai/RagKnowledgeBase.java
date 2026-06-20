package com.edwardjones.demo.ai;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * In-memory RAG knowledge snippets for the demo. In a production system
 * these would live in a vector store with embedding-based retrieval — for
 * this POC, a small fixed list is injected into every explanation prompt.
 *
 * Per 05-ai-explanation-service.md.
 */
@Component
public class RagKnowledgeBase {

    private static final List<String> SNIPPETS = List.of(
        "Tax-loss harvesting allows investors to sell securities at a loss " +
        "to offset capital gains, potentially reducing tax liability. The IRS " +
        "wash-sale rule prohibits repurchasing substantially identical securities " +
        "within 30 days.",

        "Long-term capital gains (assets held more than 1 year) are taxed at " +
        "preferential rates of 0%, 15%, or 20% for most taxpayers, plus the 3.8% " +
        "Net Investment Income Tax for high earners.",

        "Short-term capital gains are taxed as ordinary income. For HNW clients " +
        "in the top bracket this can be 37% federal plus state taxes.",

        "Tax-lot selection (specific identification method) allows investors to " +
        "choose which lot to sell, potentially minimizing gain recognition or " +
        "maximizing harvestable losses.",

        "Asset location — placing tax-inefficient assets in tax-deferred accounts " +
        "— is a separate but complementary strategy to tax-loss harvesting."
    );

    /**
     * Returns all snippets. For this demo every snippet is injected into
     * every prompt — no retrieval/ranking step. A production RAG system
     * would select only the top-K most relevant snippets per query.
     */
    public List<String> allSnippets() {
        return SNIPPETS;
    }

    public String asFormattedContext() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SNIPPETS.size(); i++) {
            sb.append(i + 1).append(". ").append(SNIPPETS.get(i)).append("\n\n");
        }
        return sb.toString().trim();
    }
}
