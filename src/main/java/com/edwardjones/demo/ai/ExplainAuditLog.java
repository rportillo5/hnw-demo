package com.edwardjones.demo.ai;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit record for every AI explanation request/response.
 * Compliance requirement from 05-ai-explanation-service.md — every
 * call to TaxExplanationService must be logged.
 */
@Entity
@Table(name = "explain_audit_log")
public class ExplainAuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "lot_id", nullable = false)
    private UUID lotId;

    @Column(name = "prompt_hash", nullable = false)
    private String promptHash;

    @Column(name = "response_text", nullable = false)
    private String responseText;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ExplainAuditLog() {
    }

    public ExplainAuditLog(UUID lotId, String promptHash, String responseText,
                             String modelVersion, Instant createdAt) {
        this.lotId = lotId;
        this.promptHash = promptHash;
        this.responseText = responseText;
        this.modelVersion = modelVersion;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getLotId() { return lotId; }
    public String getPromptHash() { return promptHash; }
    public String getResponseText() { return responseText; }
    public String getModelVersion() { return modelVersion; }
    public Instant getCreatedAt() { return createdAt; }
}
