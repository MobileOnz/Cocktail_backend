package com.application.common.auth.config;

import com.application.common.auth.JWTAccessTokenBlackListService;
import com.application.common.auth.jwt.JWTFilter;
import com.application.common.auth.jwt.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] SWAGGER_URLS = {
            "/swagger-ui.html", // 메인 UI 페이지
            "/swagger-ui/**",   // UI 리소스 (js, css)
            "/v3/api-docs",
            "/v3/api-docs/**"   // API 설계도(JSON)
    };

    private final JWTUtil jwtUtil;
    private final JWTAccessTokenBlackListService jwtAccessTokenBlackListService;

    public SecurityConfig(JWTUtil jwtUtil, JWTAccessTokenBlackListService jwtAccessTokenBlackListService){
        this.jwtUtil = jwtUtil;
        this.jwtAccessTokenBlackListService = jwtAccessTokenBlackListService;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurity(HttpSecurity http) throws Exception {
        http
                .csrf((auth) -> auth.disable())
                .securityMatcher("/admin/**")
                .formLogin((auth) -> auth.loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login-process")
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureUrl("/admin/login?error=true")
                        .permitAll())
                .logout((auth) -> auth.logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout=true")
                        .permitAll())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login", "/admin/login-process", "/admin/css/**", "/admin/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement((auth) -> auth.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        http
                .csrf((auth) -> auth.disable());

        http
                .formLogin((auth) -> auth.disable());

        http
                .httpBasic((auth) -> auth.disable());

        http
                .addFilterBefore(new JWTFilter(jwtUtil, jwtAccessTokenBlackListService), UsernamePasswordAuthenticationFilter.class);

        http
                .authorizeHttpRequests((auth)->auth
//                        .requestMatchers("/test/**").permitAll() // 로그인 무시하고 api 요청 테스트 하기 위한
                        .requestMatchers("/api/auth/refresh", "/api/auth/social-login", "/api/auth/**").permitAll()
                        .requestMatchers("/onz/api/auth/refresh", "/onz/api/auth/social-login", "/onz/api/auth/**").permitAll()
                        .requestMatchers("/login/oauth2/code/google", "/login/oauth2/code/naver", "/login/oauth2/code/kakao", "/login/oauth2/code/apple").permitAll()
                        .requestMatchers("/onz/login/oauth2/code/google", "/onz/login/oauth2/code/naver", "/onz/login/oauth2/code/kakao", "/onz/login/oauth2/code/apple").permitAll()
                        .requestMatchers("/api/auth/naver/token", "/api/auth/google/token", "/api/auth/kakao/token", "/api/auth/apple/token").permitAll()
                        .requestMatchers("/api/auth/naver/login-url", "/api/auth/google/login-url", "/api/auth/kakao/login-url", "/api/auth/apple/login-url").permitAll()
                        .requestMatchers("/api/location/**", "/api/search/**", "/api/bar/**", "/api/item/public/**").permitAll()
                        .requestMatchers("/api/public/**", "/.well-known/acme-challenge/**" ,"/error", "/images/**").permitAll()
                        .requestMatchers("/uploads/**", "/onz/uploads/**").permitAll()
                        // swagger
                        .requestMatchers(SWAGGER_URLS).permitAll()
                        .requestMatchers("/webjars/**", "/favicon.ico").permitAll()
                        // onz_v2 - JWT 필터 화이트리스트 방식에 맞춰 v2 API는 기본 허용
                        .requestMatchers("/api/v2/**").permitAll()
                        .requestMatchers("/onz/api/v2/**").permitAll()
                        .anyRequest().authenticated());

        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));


        return http.build();
    }


    /**
     * Admin 계정 자격증명을 환경변수에서 읽어옴.
     * - ADMIN_USERNAME (default: "admin")
     * - ADMIN_PASSWORD (default: "admin1!" — 운영에선 반드시 .env 로 덮어쓸 것)
     * 운영 EC2 의 .env 에서 덮어쓰면 컨테이너 재시작 시 즉시 반영.
     */
    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${admin.username:admin}") String adminUsername,
            @Value("${admin.password:admin1!}") String adminPassword
    ) {
        var admin = User.withUsername(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}