package com.dealer.dealer_inventory.audit.service;

import com.dealer.dealer_inventory.audit.entity.AuditAction;
import com.dealer.dealer_inventory.audit.entity.AuditLog;
import com.dealer.dealer_inventory.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private AuditService auditService;

    @Test
    void saveLog_persistsAuditLog() {
        AuditLog log = AuditLog.builder()
                .tenantId("t1")
                .httpMethod("GET")
                .endpoint("/dealers")
                .action(AuditAction.LIST)
                .responseStatus(200)
                .durationMs(50L)
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(log);

        auditService.saveLog(log);

        verify(auditLogRepository).save(log);
    }

    @Test
    void saveLog_exceptionDoesNotPropagate() {
        AuditLog log = AuditLog.builder()
                .httpMethod("GET")
                .endpoint("/dealers")
                .build();

        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        // Should not throw — exception is caught and logged
        auditService.saveLog(log);

        verify(auditLogRepository).save(log);
    }
}

