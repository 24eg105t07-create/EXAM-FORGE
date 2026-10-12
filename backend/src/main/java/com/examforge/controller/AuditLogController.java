package com.examforge.controller;

import com.examforge.dto.AuditLogDto;
import com.examforge.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/audit-logs")
@PreAuthorize("hasRole('MANAGEMENT')")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "System-wide immutable audit trail for sensitive administrative operations")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Get audit logs (Management only, paginated)")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(@PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAuditLogs(pageable));
    }
}
