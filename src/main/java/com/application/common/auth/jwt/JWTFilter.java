package com.application.common.auth.jwt;


import com.application.common.Constant;
import com.application.common.auth.JWTAccessTokenBlackListService;
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
import java.util.List;


@RequiredArgsConstructor
@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final JWTAccessTokenBlackListService jwtAccessTokenBlackListService;

    // [기존 방식 - 주석 처리] JWT 필터 예외 경로 (기본 차단, 예외만 허용)
//    private final static List<String> EXCLUDE_PATH =  List.of(
//        "/api/auth/refresh", "/api/auth/social-login","/api/auth/signup",
//        "/login/oauth2/code/google", "/login/oauth2/code/naver", "/login/oauth2/code/kakao",
//        "/api/auth/naver/token", "/api/auth/google/token", "/api/auth/kakao/token",
//        "/api/auth/naver/login-url", "/api/auth/google/login-url", "/api/auth/kakao/login-url",
//        "/api/public/", "/.well-known/acme-challenge/",
//        "/admin/cocktail/excel/upload","/admin/cocktail/excel/download", "/admin/cocktail/excel"
//    );

    // [기존 방식 - 주석 처리] JWT 필터 예외 경로 (토큰 없이 접근 가능)
//    private final static List<String> EXCLUDE_PATH_PATTERNS = List.of(
//            // 공개 API
//            "^/api/public/.*",                      // 공개 API (인증 불필요)
//            "^/api/v2/public/.*",                   // 공개 API v2 (칵테일, 모니터링)
//            "^/onz/api/v2/public/.*",               // 공개 API v2 (onz 경로)
//
//            // SSL 인증서 관련
//            "^/.well-known/acme-challenge/.*",      // Let's Encrypt SSL 인증서 검증
//
//            // 정적 리소스
//            "^/images/.*",                          // 이미지 파일
//            "^/error$",                             // 에러 페이지
//            "^/favicon\\.ico$",                     // 파비콘
//
//            // Swagger (API 문서)
//            "^/onz/swagger-ui/.*",                  // Swagger UI (onz 경로)
//            "^/swagger-ui/.*",                      // Swagger UI
//            "^/onz/v3/api-docs.*",                  // Swagger API Docs (onz 경로)
//            "^/v3/api-docs.*",                      // Swagger API Docs
//            "^/webjars/.*",                         // Swagger UI 리소스
//
//            // 인증 API (v1 - 기존)
//            "^/api/auth/.*",                        // 기존 인증 API 전체
//            "^/onz/api/auth/.*",                    // 기존 인증 API 전체 (onz 경로)
//
//            // 인증 API (v2 - 신규)
//            "^/api/v2/auth/social-login$",          // 소셜 로그인
//            "^/api/v2/auth/signup$",                // 회원가입
//            "^/api/v2/auth/reissue$",               // 토큰 재발급
//            "^/onz/api/v2/auth/social-login$",      // 소셜 로그인 (onz 경로)
//            "^/onz/api/v2/auth/signup$",            // 회원가입 (onz 경로)
//            "^/onz/api/v2/auth/reissue$",           // 토큰 재발급 (onz 경로)
//
//            // OAuth2 콜백
//            "^/login/oauth2/code/.*",               // OAuth2 콜백 (Google, Naver, Kakao, Apple)
//            "^/onz/login/oauth2/code/.*"            // OAuth2 콜백 (onz 경로)
//    );
//
//    private final static List<String> OPTIONAL_AUTH_PATH_PATTERNS = List.of(
//            "^/api/location/.*",
//            "^/api/search/.*",
//            "^/api/bar/.*",
//            "^/api/item/public/.*"
//    );

    // [새로운 방식 - 기존의 예외 방식이 아닌 jwt 필터가 필요한 부분만 적용] JWT 필터 적용 경로 (기본 허용, 특정 경로만 인증 필요)
    private final static List<String> REQUIRE_AUTH_PATH_PATTERNS = List.of(
            // 회원 관련 API (모두 인증 필요)
            "^/api/v2/members/.*",                      // 회원 정보 조회, 수정, 탈퇴, 프로필 관리
            "^/onz/api/v2/members/.*",                  // 회원 API (onz 경로)

            // 인증 API (로그아웃만 인증 필요)
            "^/api/v2/auth/logout$",                    // 로그아웃
            "^/onz/api/v2/auth/logout$",                // 로그아웃 (onz 경로)

            // 칵테일 반응 API (인증 필요)
            "^/api/v2/cocktails/[0-9]+/reactions$",     // 칵테일 반응 조회/토글
            "^/onz/api/v2/cocktails/[0-9]+/reactions$", // 칵테일 반응 (onz 경로)

            // 칵테일 북마크 API (인증 필요)
            "^/api/v2/cocktails/[0-9]+/bookmarks$",     // 칵테일 북마크 토글
            "^/api/v2/cocktails/bookmarks$",            // 내 북마크 목록 조회
            "^/onz/api/v2/cocktails/[0-9]+/bookmarks$", // 칵테일 북마크 토글 (onz 경로)
            "^/onz/api/v2/cocktails/bookmarks$"         // 내 북마크 목록 조회 (onz 경로)
    );

    // 선택적 인증 경로 (JWT 토큰이 있으면 검증하고, 없으면 익명 사용자로 통과)
    private final static List<String> OPTIONAL_AUTH_PATH_PATTERNS = List.of(
            "^/api/v2/cocktails$",                      // 칵테일 목록 조회 (북마크 여부 포함)
            "^/api/v2/cocktails/detail$",               // 칵테일 상세 조회 (북마크 여부 포함)
            "^/api/v2/cocktails/best$",                 // BEST 칵테일 조회 (북마크 여부 포함)
            "^/api/v2/cocktails/recent$",               // 최근 칵테일 조회 (북마크 여부 포함)
            "^/api/v2/cocktails/specific$",             // 특정 칵테일 조회 (북마크 여부 포함)
            "^/api/v2/cocktails/refresh$",              // 상큼한 칵테일 추천 (북마크 여부 포함)
            "^/onz/api/v2/cocktails$",                  // 칵테일 목록 조회 (onz 경로)
            "^/onz/api/v2/cocktails/detail$",           // 칵테일 상세 조회 (onz 경로)
            "^/onz/api/v2/cocktails/best$",             // BEST 칵테일 조회 (onz 경로)
            "^/onz/api/v2/cocktails/recent$",           // 최근 칵테일 조회 (onz 경로)
            "^/onz/api/v2/cocktails/specific$",         // 특정 칵테일 조회 (onz 경로)
            "^/onz/api/v2/cocktails/refresh$"           // 상큼한 칵테일 추천 (onz 경로)
    );

    // [기존 방식 : jwt 예외 필터 적용 - 주석 처리]
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//
//        if(isExcludedPath(request.getRequestURI())){
//            log.info("Skipping JWTFilter : {}", request.getRequestURI());
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        String accessToken = getAccessToken(request);
//
//        if (isOptionalAuthPath(request.getRequestURI())) {
//            if (accessToken != null && !jwtUtil.isAccessExpired(accessToken)) {
//                String uuid = jwtUtil.getUUID(accessToken);
//                String blackListToken = jwtAccessTokenBlackListService.getAccessTokenFromBlackList(uuid);
//
//                if (blackListToken == null) {
//                    setSecurityContext(jwtUtil, accessToken);
//                    log.info("Optional path - SecurityContext set for accessToken UUID: {}", uuid);
//                } else {
//                    log.info("Optional path - Access Token is blacklisted, skip auth");
//                }
//            } else {
//                log.info("Optional path - No valid accessToken, proceed as anonymous");
//            }
//
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        if(accessToken == null){
//            log.info("accessToken null");
//            sendErrorResponse(response, Constant.ERROR_CODE, HttpServletResponse.SC_BAD_REQUEST, "Access Token 값이 헤더에 존재하지 않음");
//            return;
//        }
//
//        if(jwtUtil.isAccessExpired(accessToken)){
//            log.info("Access Token expire");
//            sendErrorResponse(response, Constant.NEED_REFRESH_TOKEN_CODE, HttpServletResponse.SC_UNAUTHORIZED,"Access Token Expired");
//            return;
//        }
//
//        log.info("jwt filter [accessToken UUID] : {}", jwtUtil.getUUID(accessToken));
//        String blackListToken = jwtAccessTokenBlackListService.getAccessTokenFromBlackList(jwtUtil.getUUID(accessToken));
//
//        if(blackListToken != null){
//            log.info("Access Token already logout");
//            sendErrorResponse(response, Constant.ERROR_CODE, HttpServletResponse.SC_UNAUTHORIZED, "Already logout member");
//            return;
//        }
//
//        log.info("Access Token is valid");
//        setSecurityContext(jwtUtil, accessToken);
//        filterChain.doFilter(request, response);
//
//    }
//
//    private Boolean isExcludedPath(String uri){
//        if (EXCLUDE_PATH.contains(uri) || EXCLUDE_PATH_PATTERNS.stream().anyMatch(uri::matches)){
//            return true;
//        }
//        return false;
//    }
//
//    private boolean isOptionalAuthPath(String uri) {
//        return OPTIONAL_AUTH_PATH_PATTERNS.stream().anyMatch(uri::matches);
//    }

    // [새로운 방식] 기본 허용, 특정 경로만 인증 필요
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. 선택적 인증 경로 처리 (JWT 토큰이 있으면 검증, 없으면 통과)
        if (isOptionalAuthPath(uri)) {
            log.info("Optional auth path: {}", uri);
            String accessToken = getAccessToken(request);

            // JWT 토큰이 있으면 검증하고 SecurityContext 설정
            if (accessToken != null && !jwtUtil.isAccessExpired(accessToken)) {
                String uuid = jwtUtil.getUUID(accessToken);
                String blackListToken = jwtAccessTokenBlackListService.getAccessTokenFromBlackList(uuid);

                if (blackListToken == null) {
                    setSecurityContext(jwtUtil, accessToken);
                    log.info("Optional path - SecurityContext set for accessToken UUID: {}", uuid);
                } else {
                    log.info("Optional path - Access Token is blacklisted, proceed as anonymous");
                }
            } else {
                log.info("Optional path - No valid accessToken, proceed as anonymous");
            }

            filterChain.doFilter(request, response);
            return;
        }

        // 2. 인증이 필요한 경로인지 확인
        if (!isRequireAuthPath(uri)) {
            // 인증이 필요하지 않은 경로 -> 바로 통과
            log.info("Public path, skipping JWT validation: {}", uri);
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 인증이 필요한 경로 -> JWT 토큰 검증 (필수)
        log.info("Protected path, validating JWT: {}", uri);

        String accessToken = getAccessToken(request);

        if(accessToken == null){
            log.info("accessToken null");
            sendErrorResponse(response, Constant.ERROR_CODE, HttpServletResponse.SC_BAD_REQUEST, "Access Token 값이 헤더에 존재하지 않음");
            return;
        }

        if(jwtUtil.isAccessExpired(accessToken)){
            log.info("Access Token expire");
            sendErrorResponse(response, Constant.NEED_REFRESH_TOKEN_CODE, HttpServletResponse.SC_UNAUTHORIZED,"Access Token Expired");
            return;
        }

        log.info("jwt filter [accessToken UUID] : {}", jwtUtil.getUUID(accessToken));
        String blackListToken = jwtAccessTokenBlackListService.getAccessTokenFromBlackList(jwtUtil.getUUID(accessToken));

        if(blackListToken != null){
            log.info("Access Token already logout");
            sendErrorResponse(response, Constant.ERROR_CODE, HttpServletResponse.SC_UNAUTHORIZED, "Already logout member");
            return;
        }

        log.info("Access Token is valid");
        setSecurityContext(jwtUtil, accessToken);
        filterChain.doFilter(request, response);

    }

    // 인증이 필요한 경로인지 확인
    private boolean isRequireAuthPath(String uri) {
        return REQUIRE_AUTH_PATH_PATTERNS.stream().anyMatch(uri::matches);
    }

    // 선택적 인증 경로인지 확인
    private boolean isOptionalAuthPath(String uri) {
        return OPTIONAL_AUTH_PATH_PATTERNS.stream().anyMatch(uri::matches);
    }

    private void sendErrorResponse(HttpServletResponse response,int code,  int status, String message) throws IOException{
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\": "+ code +", \"msg\": \""+ message +"\", \"data\" : \"null\"}");
        response.flushBuffer();
    }



    private String getAccessToken(HttpServletRequest request){
        String accessToken = request.getHeader("Authorization");
        if (accessToken == null){
            return accessToken;
        }else{
            accessToken = accessToken.substring(7);
            return accessToken;
        }

    }


    private void setSecurityContext(JWTUtil jwtUtil, String accessToken) {
        String credentialId = jwtUtil.getCredentialId(accessToken);
        String role = jwtUtil.getRole(accessToken);

        UserDTO userDTO = new UserDTO(credentialId, role);
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(userDTO);

        Authentication authToken = new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

    }
}
