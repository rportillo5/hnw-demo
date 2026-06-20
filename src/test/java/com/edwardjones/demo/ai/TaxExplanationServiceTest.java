package com.edwardjones.demo.ai;

import com.edwardjones.demo.config.AnthropicConfig;
import com.edwardjones.demo.domain.TaxLot;
import com.edwardjones.demo.domain.TaxLotRepository;
import com.edwardjones.demo.tax.TaxRulesEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TaxExplanationService. AnthropicClient is mocked so these
 * tests run instantly with no real API calls, no API key needed, and no cost.
 */
@ExtendWith(MockitoExtension.class)
class TaxExplanationServiceTest {

    @Mock private AnthropicClient anthropicClient;
    @Mock private AnthropicConfig anthropicConfig;
    @Mock private TaxLotRepository taxLotRepository;
    @Mock private ExplainAuditLogRepository auditLogRepository;

    private TaxExplanationService service;
    private final RagKnowledgeBase knowledgeBase = new RagKnowledgeBase();
    private final TaxRulesEngine taxRulesEngine = new TaxRulesEngine();

    private static final UUID LOT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TaxExplanationService(
            anthropicClient, anthropicConfig, knowledgeBase,
            taxLotRepository, taxRulesEngine, auditLogRepository
        );
    }

    @Test
    @DisplayName("Explanation includes disclaimer when model already includes it")
    void explanation_modelIncludesDisclaimer_notDuplicated() {
        when(anthropicConfig.getModel()).thenReturn("claude-sonnet-4-6");
        TaxLot lot = tslaLotA();
        when(taxLotRepository.findById(LOT_ID)).thenReturn(Optional.of(lot));

        String modelResponseWithDisclaimer =
            "Tesla Lot A has an unrealized loss. " + TaxExplanationService.DISCLAIMER;
        when(anthropicClient.complete(anyString(), anyString()))
            .thenReturn(modelResponseWithDisclaimer);

        ExplainResponse result = service.explain(LOT_ID, "harvest");

        // Disclaimer should appear exactly once, not duplicated
        int occurrences = countOccurrences(result.explanation(), TaxExplanationService.DISCLAIMER);
        assertThat(occurrences).isEqualTo(1);
        assertThat(result.disclaimer()).isEqualTo(TaxExplanationService.DISCLAIMER);
    }

    @Test
    @DisplayName("Explanation gets disclaimer appended when model omits it")
    void explanation_modelOmitsDisclaimer_appendedServerSide() {
        when(anthropicConfig.getModel()).thenReturn("claude-sonnet-4-6");
        TaxLot lot = tslaLotA();
        when(taxLotRepository.findById(LOT_ID)).thenReturn(Optional.of(lot));

        String modelResponseWithoutDisclaimer = "Tesla Lot A has an unrealized loss of $3,325.";
        when(anthropicClient.complete(anyString(), anyString()))
            .thenReturn(modelResponseWithoutDisclaimer);

        ExplainResponse result = service.explain(LOT_ID, "harvest");

        assertThat(result.explanation()).contains(TaxExplanationService.DISCLAIMER);
        assertThat(result.explanation()).startsWith(modelResponseWithoutDisclaimer);
    }

    @Test
    @DisplayName("Every explanation call writes an audit log entry")
    void explanation_writesAuditLog() {
        when(anthropicConfig.getModel()).thenReturn("claude-sonnet-4-6");
        TaxLot lot = tslaLotA();
        when(taxLotRepository.findById(LOT_ID)).thenReturn(Optional.of(lot));
        when(anthropicClient.complete(anyString(), anyString())).thenReturn("Some explanation.");

        service.explain(LOT_ID, "harvest");

        org.mockito.Mockito.verify(auditLogRepository).save(any(ExplainAuditLog.class));
    }

    @Test
    @DisplayName("Unknown lotId throws NoSuchElementException, no API call made")
    void unknownLotId_throwsException_noApiCall() {
        UUID unknownId = UUID.randomUUID();
        when(taxLotRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.explain(unknownId, "harvest"))
            .isInstanceOf(NoSuchElementException.class);

        org.mockito.Mockito.verifyNoInteractions(anthropicClient);
    }

    @Test
    @DisplayName("User message passed to Claude includes the computed tax savings, not raw LLM math")
    void userMessage_includesPrecomputedSavings() {
        when(anthropicConfig.getModel()).thenReturn("claude-sonnet-4-6");
        TaxLot lot = tslaLotA();
        when(taxLotRepository.findById(LOT_ID)).thenReturn(Optional.of(lot));
        when(anthropicClient.complete(anyString(), anyString())).thenReturn("Explanation text.");

        service.explain(LOT_ID, "harvest");

        org.mockito.ArgumentCaptor<String> userMessageCaptor =
            org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(anthropicClient).complete(anyString(), userMessageCaptor.capture());

        String capturedMessage = userMessageCaptor.getValue();
        // TSLA Lot A: loss = -3325.00, savings = 791.35 (computed by TaxRulesEngine, not the LLM)
        assertThat(capturedMessage).contains("TSLA");
        assertThat(capturedMessage).contains("-3325.00");
        assertThat(capturedMessage).contains("791.35");
    }

    private int countOccurrences(String text, String substring) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(substring, idx)) != -1) {
            count++;
            idx += substring.length();
        }
        return count;
    }

    private TaxLot tslaLotA() {
        return new TaxLot(
            LOT_ID, UUID.randomUUID(), "TSLA",
            LocalDate.of(2023, 3, 15), new BigDecimal("50"),
            new BigDecimal("245.00"), new BigDecimal("178.50"), "Lot A"
        );
    }
}
