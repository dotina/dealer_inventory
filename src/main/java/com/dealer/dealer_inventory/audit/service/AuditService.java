package com.dealer.dealer_inventory.audit.service;

import com.dealer.dealer_inventory.audit.entity.AuditLog;
import com.dealer.dealer_inventory.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit log entries asynchronously so the main request thread is never blocked.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Never let audit failures propagate to the caller
            log.error("Failed to persist audit log: {}", e.getMessage(), e);
        }
    }
}

