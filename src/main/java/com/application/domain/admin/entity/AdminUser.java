package com.application.domain.admin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 계정. 세션 폼로그인 대상.
 * 과거 SecurityConfig 의 인메모리 하드코딩(admin/admin1!, F-03)을 대체한다.
 */
@Entity
@Table(name = "admin_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String role;   // ADMIN | SUPER

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AdminUser(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.role == null) {
            this.role = "ADMIN";
        }
    }
}
