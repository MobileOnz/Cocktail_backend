package com.application.domain.admin.service;

import com.application.domain.admin.entity.AdminUser;
import com.application.domain.admin.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 부팅 시 관리자 시드. V2 마이그레이션이 아니라 여기서 만든다(자격증명을 SQL에 넣지 않기 위해, F-03).
 *
 * 규칙:
 *  - ADMIN_USERNAME/ADMIN_PASSWORD env 가 모두 있고 해당 username 이 아직 없으면 BCrypt 로 생성.
 *  - env 가 없으면 아무것도 하지 않는다(관리자 미생성 → 로그인 불가, 안전한 기본값).
 *  - 이미 존재하면 건드리지 않는다(비밀번호 덮어쓰기 금지).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserBootstrap implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:}")
    private String adminUsername;

    @Value("${admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_USERNAME/ADMIN_PASSWORD 미설정 — 관리자 시드 건너뜀 (관리자 로그인 불가).");
            return;
        }
        if (adminUserRepository.existsByUsername(adminUsername)) {
            log.info("관리자 계정 '{}' 이미 존재 — 시드 건너뜀.", adminUsername);
            return;
        }
        AdminUser admin = AdminUser.builder()
                .username(adminUsername)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role("SUPER")
                .build();
        adminUserRepository.save(admin);
        log.info("관리자 계정 '{}' 부트스트랩 생성 완료.", adminUsername);
    }
}
