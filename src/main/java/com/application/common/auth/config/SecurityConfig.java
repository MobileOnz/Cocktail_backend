package com.application.common.auth.config;

import com.application.common.auth.jwt.JWTFilter;
import com.application.common.auth.jwt.JWTUtil;
import com.application.web.services.auth.JWTAccessTokenBlackListService;
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
                        .requestMatchers("/api/auth/refresh", "/api/auth/social-login", "/api/auth/**").permitAll()
                        .requestMatchers("/login/oauth2/code/google", "/login/oauth2/code/naver", "/login/oauth2/code/kakao").permitAll()
                        .requestMatchers("/api/auth/naver/token", "/api/auth/google/token", "/api/auth/kakao/token").permitAll()
                        .requestMatchers("/api/auth/naver/login-url", "/api/auth/google/login-url", "/api/auth/kakao/login-url").permitAll()
                        .requestMatchers("/api/location/**", "/api/search/**", "/api/bar/**", "/api/item/public/**").permitAll()
                        .requestMatchers("/api/public/**", "/.well-known/acme-challenge/**" ,"/error", "/images/**").permitAll()
                        .anyRequest().authenticated());

        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));


        return http.build();
    }


    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var admin = User.withUsername("admin").password(passwordEncoder.encode("admin1!")).roles("ADMIN").build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
