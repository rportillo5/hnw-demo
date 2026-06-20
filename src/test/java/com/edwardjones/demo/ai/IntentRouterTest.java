package com.edwardjones.demo.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntentRouterTest {

    @Mock
    private AnthropicClient anthropicClient;

    private IntentRouter router;

    @BeforeEach
    void setUp() {
        router = new IntentRouter(anthropicClient);
    }

    @Test
    @DisplayName("'explain' response → EXPLAIN intent")
    void explain_intent() {
        when(anthropicClient.complete(anyString(), anyString())).thenReturn("explain");
        assertThat(router.classify("what does this lot mean?")).isEqualTo(Intent.EXPLAIN);
    }

    @Test
    @DisplayName("'whatif' response → WHATIF intent")
    void whatif_intent() {
        when(anthropicClient.complete(anyString(), anyString())).thenReturn("whatif");
        assertThat(router.classify("what if I sell this and buy VTI?")).isEqualTo(Intent.WHATIF);
    }

    @Test
    @DisplayName("'compare' response → COMPARE intent")
    void compare_intent() {
        when(anthropicClient.complete(anyString(), anyString())).thenReturn("compare");
        assertThat(router.classify("compare lot A and lot B")).isEqualTo(Intent.COMPARE);
    }

    @Test
    @DisplayName("'portfolio_summary' response → PORTFOLIO_SUMMARY intent")
    void portfolio_summary_intent() {
        when(anthropicClient.complete(anyString(), anyString())).thenReturn("portfolio_summary");
        assertThat(router.classify("summarize the portfolio")).isEqualTo(Intent.PORTFOLIO_SUMMARY);
    }

    @Test
    @DisplayName("'unsupported' response → UNSUPPORTED intent")
    void unsupported_intent() {
        when(anthropicClient.complete(anyString(), anyString())).thenReturn("unsupported");
        assertThat(router.classify("should I sell everything?")).isEqualTo(Intent.UNSUPPORTED);
    }

    @Test
    @DisplayName("Unrecognised model response → UNSUPPORTED fallback")
    void unrecognised_response_fallsBackToUnsupported() {
        when(anthropicClient.complete(anyString(), anyString())).thenReturn("something_random");
        assertThat(router.classify("some query")).isEqualTo(Intent.UNSUPPORTED);
    }

    @Test
    @DisplayName("Model response with extra whitespace is cleaned before matching")
    void response_withWhitespace_cleanedCorrectly() {
        when(anthropicClient.complete(anyString(), anyString())).thenReturn("  explain  \n");
        assertThat(router.classify("explain this")).isEqualTo(Intent.EXPLAIN);
    }
}
