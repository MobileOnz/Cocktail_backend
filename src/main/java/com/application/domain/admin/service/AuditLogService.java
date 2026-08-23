package com.application.domain.admin.service;

import com.application.domain.admin.entity.AdminAuditLog;
import com.application.domain.admin.repository.AdminAuditLogRepository;
import com.application.domain.admin.repository.AdminUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 관리자 쓰기 작업 감사 로그 기록기. 컨트롤러/서비스가 CRUD 성공 직후 호출한다.
 * 현재 로그인 관리자를 SecurityContext 에서 해석해 admin_id 로 남긴다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AdminAuditLogRepository auditLogRepository;
    private final AdminUserRepository adminUserRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 변경 전/후 객체를 JSON 직렬화해 감사 로그를 남긴다. before/after 는 null 허용. */
    public void log(String action, String entityType, Object entityId, Object before, Object after) {
        Long adminId = currentAdminId();
        AdminAuditLog entry = AdminAuditLog.builder()
                .adminId(adminId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId == null ? null : String.valueOf(entityId))
                .beforeJson(toJson(before))
                .afterJson(toJson(after))
                .build();
        auditLogRepository.save(entry);
    }

    private Long currentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return adminUserRepository.findByUsername(auth.getName())
                .map(u -> u.getId())
                .orElse(null);
    }

    private String toJson(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            log.warn("audit JSON 직렬화 실패 ({}): {}", o.getClass().getSimpleName(), e.getMessage());
            return "\"<serialization-failed>\"";
        }
    }
}
