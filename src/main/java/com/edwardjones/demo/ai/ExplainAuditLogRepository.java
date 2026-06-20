package com.edwardjones.demo.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExplainAuditLogRepository extends JpaRepository<ExplainAuditLog, UUID> {
}
