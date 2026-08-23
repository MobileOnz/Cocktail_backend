package com.application.common.auth.jwt;


import com.application.common.Constant;
import com.application.common.auth.JWTAccessTokenBlackListService;
import com.application.common.auth.config.EndpointAuthorizationPolicy;
import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.auth.dto.oauth2Dto.UserDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/**
 * JWT 인증 필터 — fail-closed (기본 차단) 방식.
 *
 * <p>과거에는 "기본 허용 + REQUIRE 화이트리스트"(fail-open)였다. 신규 엔드포인트가
 * 화이트리스트 등록을 잊는 순간 무인증 공개되는 구조였고, 실제로 검색기록 API에서
 * 한 번 무너졌다(audit F-02, F-08). 이제는 다음 규칙으로 뒤집는다.</p>
 *
 * <ol>
 *   <li><b>PUBLIC</b> — 명시된 공개 경로만 토큰 없이 통과. SecurityContext 미설정.</li>
 *   <li><b>OPTIONAL</b> — 토큰이 있으면 검증해 컨텍스트를 설정하고, 없으면 익명 통과.</li>
 *   <li><b>그 외 전부(기본값)</b> — 토큰 필수. 없거나 무효면 401.</li>
 * </ol>
 *
 * <p>등급 판정과 경로 정규화(/onz 접두사 제거)는 {@link EndpointAuthorizationPolicy}에 위임한다.
 * 그 정책 클래스는 계약 테스트(T-05)와 <b>공유</b>되므로, 필터가 허용하는 것과 테스트가
 * 검증하는 것이 절대 어긋나지 않는다.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final JWTAccessTokenBlackListService jwtAccessTokenBlackListService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String path = EndpointAuthorizationPolicy.normalize(request.getRequestURI());
        final EndpointAuthorizationPolicy.Tier tier = EndpointAuthorizationPolicy.classify(path);

        // 1) 공개 경로 — 토큰 검증 없이 통과
        if (tier == EndpointAuthorizationPolicy.Tier.PUBLIC) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = getAccessToken(request);

        // 2) 선택적 인증 경로 — 유효 토큰이 있으면 컨텍스트 설정, 없거나 무효면 익명 통과
        if (tier == EndpointAuthorizationPolicy.Tier.OPTIONAL) {
            if (accessToken != null && !jwtUtil.isAccessExpired(accessToken)) {
                String uuid = jwtUtil.getUUID(accessToken);
                if (jwtAccessTokenBlackListService.getAccessTokenFromBlackList(uuid) == null) {
                    setSecurityContext(jwtUtil, accessToken);
                }
            }
            filterChain.doFilter(request, response);
            return;
        }

        // 3) 그 외 전부 — 토큰 필수 (fail-closed 기본값)
        if (accessToken == null) {
            sendErrorResponse(response, Constant.ERROR_CODE, HttpServletResponse.SC_UNAUTHORIZED,
                    "인증이 필요합니다.");
            return;
        }

        if (jwtUtil.isAccessExpired(accessToken)) {
            sendErrorResponse(response, Constant.NEED_REFRESH_TOKEN_CODE, HttpServletResponse.SC_UNAUTHORIZED,
                    "Access Token Expired");
            return;
        }

        String blackListToken = jwtAccessTokenBlackListService.getAccessTokenFromBlackList(jwtUtil.getUUID(accessToken));
        if (blackListToken != null) {
            sendErrorResponse(response, Constant.ERROR_CODE, HttpServletResponse.SC_UNAUTHORIZED,
                    "이미 로그아웃된 사용자입니다.");
            return;
        }

        setSecurityContext(jwtUtil, accessToken);
        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, int code, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\": " + code + ", \"msg\": \"" + message + "\", \"data\" : null}");
        response.flushBuffer();
    }

    /**
     * Authorization 헤더에서 토큰 추출. "Bearer " 접두사가 있으면 제거하고, 없으면 값을
     * 그대로 사용한다. 과거 무조건 {@code substring(7)} 하던 코드는 7자 미만 헤더에서
     * 500(StringIndexOutOfBounds)을 유발했다(audit F-06). null/길이 안전하게 처리한다.
     */
    private String getAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return null;
        }
        if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }
        return header.trim();
    }

    private void setSecurityContext(JWTUtil jwtUtil, String accessToken) {
        String credentialId = jwtUtil.getCredentialId(accessToken);
        String role = jwtUtil.getRole(accessToken);

        UserDTO userDTO = new UserDTO(credentialId, role);
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(userDTO);

        Authentication authToken =
                new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
