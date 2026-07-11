package com.application.domain.admin.service;

import com.application.domain.admin.entity.AdminUser;
import com.application.domain.admin.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * admin_user 테이블 기반 인증. 과거의 InMemoryUserDetailsManager(admin/admin1! 하드코딩, F-03)를 대체.
 * 모든 관리자는 ROLE_ADMIN 을 가지며, role=SUPER 이면 ROLE_SUPER 도 부여한다.
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser admin = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("관리자 계정을 찾을 수 없습니다: " + username));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if ("SUPER".equalsIgnoreCase(admin.getRole())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER"));
        }

        return User.withUsername(admin.getUsername())
                .password(admin.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
