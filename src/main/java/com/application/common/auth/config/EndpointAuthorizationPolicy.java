package com.application.common.auth.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 엔드포인트 인가 정책 — 단일 진실 원천(single source of truth).
 *
 * <p>{@code JWTFilter}(런타임)와 계약 테스트(T-05)가 <b>같은</b> 이 목록을 참조한다. 그래서
 * 필터가 허용하는 것과 테스트가 검증하는 것이 절대 어긋나지 않는다.</p>
 *
 * <p>규칙(fail-closed): PUBLIC/OPTIONAL 에 명시된 경로만 완화되고, 그 외 전부 토큰 필수.
 * 신규 엔드포인트는 등록 전까지 자동으로 차단된다. 경로는 base 기준(=/onz 접두사 제거 후).</p>
 */
public final class EndpointAuthorizationPolicy {

    public enum Tier { PUBLIC, OPTIONAL, AUTH_REQUIRED }

    private EndpointAuthorizationPolicy() {}

    /**
     * 인프라/정적 리소스 — @RequestMapping 컨트롤러가 아니라 프레임워크(리소스/에러) 핸들러가 처리.
     * 공개 취급하되, ghost 검사(컨트롤러 매핑 존재)에서는 제외한다.
     */
    private static final List<String> INFRA_PUBLIC = List.of(
            "^/swagger-ui.*$",
            "^/v3/api-docs.*$",
            "^/webjars/.*$",
            "^/favicon\\.ico$",
            "^/error$",
            "^/images/.*$",
            "^/\\.well-known/.*$",
            "^/$",

            // 데드코드 묘비(tombstone): 개발/테스트 전용 /api/v2/test/** 는 프로덕션에 존재하면 안 된다.
            // 과거 TestAuthController(/api/v2/test/auth/token)가 임의 회원 토큰을 무인증 발급하던
            // 계정탈취 취약점(F-01)이 있었고, 그 컨트롤러는 제거됐다. 그런데 fail-closed 기본값 때문에
            // 이 네임스페이스는 (핸들러가 없어도) 무토큰 요청에 401 을 돌려줘 "핸들러 부재"를 가려버린다.
            // 제거된 test 엔드포인트는 정직하게 404(핸들러 없음)로 떨어져야 그 부재가 계약(authz.hurl)으로
            // 고정·회귀검출된다. 그래서 이 네임스페이스만 필터를 통과시켜 DispatcherServlet 이 404 를 내게 한다.
            //   ⚠️ 트레이드오프: 이 접두사는 ghost 검사(컨트롤러 매핑 1:1)에서 제외되므로, 여기에 실제
            //   컨트롤러를 다시 두면 무인증 공개된다. /api/v2/test/** 에는 어떤 핸들러도 두지 말 것.
            "^/api/v2/test/.*$"
    );

    /**
     * API 공개 경로 — 토큰 불필요, 컨텍스트 미설정. 각 패턴은 실제 컨트롤러 매핑과 1:1이어야 한다
     * (ghost 금지: 매핑 없는 패턴은 계약 테스트가 실패시킨다).
     */
    private static final List<String> API_PUBLIC = List.of(
            // 인증 / OAuth2 (로그인 전 호출)
            "^/api/v2/auth/social-login$",
            "^/api/v2/auth/signup$",
            "^/api/v2/auth/reissue$",
            "^/api/v2/auth/[a-zA-Z]+/login-url$",       // naver/google/kakao login-url
            "^/login/oauth2/code/[a-zA-Z]+$",           // OAuth2 콜백

            // 칵테일 — 개인화 없는 순수 공개 조회
            "^/api/v2/cocktails/all$",
            "^/api/v2/cocktails/random$",
            "^/api/v2/cocktails/recommendation$",
            "^/api/v2/cocktails/suggestions$",
            "^/api/v2/cocktails/names$",
            "^/api/v2/cocktails/guide$",
            "^/api/v2/cocktails/guide/list$",
            "^/api/v2/cocktails/[0-9]+/steps$",         // T-07 제조 단계 조회
            "^/api/v2/cocktails/[0-9]+/guides$",        // T-09 이 칵테일의 이야기

            // 뉴스 (GET 조회 + 조회수 기록)
            "^/api/v2/news$",
            "^/api/v2/news/featured$",                  // T-07/08 featured
            "^/api/v2/news/[0-9]+$",
            "^/api/v2/news/[0-9]+/read$",               // T-08 조회수 증가(POST) — PUBLIC

            // 매거진 (블록형 콘텐츠, 개인화 없는 순수 공개 조회 + 조회수)
            "^/api/v2/magazine$",
            "^/api/v2/magazine/tags$",
            "^/api/v2/magazine/[0-9]+$",
            "^/api/v2/magazine/[0-9]+/read$",

            // 온보딩 / 모니터링 (로그인 전 호출)
            "^/api/v2/monitoring/track$",
            "^/api/v2/monitoring/onboarding$",
            "^/api/v2/monitoring/onboarding/status$"
            // 주의: /api/v2/monitoring/info 는 비공개 → 기본 차단(회원 PII, F-13)
    );

    /**
     * 선택적 인증 경로 — 토큰이 있으면 검증해 컨텍스트를 설정하고, 없으면 익명 통과.
     * 로그인 시 개인화(isVisited, 북마크 여부 등)를 반영해야 하는 조회 경로가 여기 온다.
     */
    private static final List<String> OPTIONAL = List.of(
            "^/api/v2/main$",                           // T-09 메인 — 토큰 있으면 개인화

            "^/api/v2/cocktails$",                      // 목록/검색(POST). 로그인 시 북마크 여부
            "^/api/v2/cocktails/detail$",
            "^/api/v2/cocktails/best$",
            "^/api/v2/cocktails/recent$",
            "^/api/v2/cocktails/refresh$",
            "^/api/v2/cocktails/beginner$",
            "^/api/v2/cocktails/intermediate$",

            // 검색기록 — 로그인 시 개인 기록, 비로그인 시 익명(빈 결과). 401 금지.
            "^/api/v2/cocktails/search/history$",
            "^/api/v2/cocktails/search/history/all$",
            "^/api/v2/cocktails/search/history/[0-9]+$",

            // 바 조회 — 로그인 시 isVisited/가격 게이팅 반영(OPTIONAL 필수. PUBLIC이면 로그인 개인화 깨짐)
            "^/api/v2/bars$",
            "^/api/v2/bars/nearby$",
            "^/api/v2/bars/[a-z0-9-]+$",
            "^/api/v2/bars/[a-z0-9-]+/menu$"            // X-Onz-Bar-Session 유무로 가격 게이팅
    );

    private static final List<Pattern> PUBLIC_PATTERNS = compile(concat(INFRA_PUBLIC, API_PUBLIC));
    private static final List<Pattern> OPTIONAL_PATTERNS = compile(OPTIONAL);

    /** context-path(/onz)를 제거해 정규 경로로 만든다. */
    public static String normalize(String uri) {
        if (uri.equals("/onz")) {
            return "/";
        }
        if (uri.startsWith("/onz/")) {
            return uri.substring(4); // "/onz/api/.." -> "/api/.."
        }
        return uri;
    }

    /** 정규화된 경로의 인가 등급을 판정한다. */
    public static Tier classify(String normalizedPath) {
        if (matchesAny(PUBLIC_PATTERNS, normalizedPath)) {
            return Tier.PUBLIC;
        }
        if (matchesAny(OPTIONAL_PATTERNS, normalizedPath)) {
            return Tier.OPTIONAL;
        }
        return Tier.AUTH_REQUIRED;
    }

    // ── 계약 테스트(T-05)용 노출 ──
    /** ghost 검사 대상 = 인프라를 제외한, 컨트롤러 매핑이 있어야 하는 공개/선택 패턴. */
    public static List<String> apiPublicPatternStrings() { return API_PUBLIC; }
    public static List<String> optionalPatternStrings() { return OPTIONAL; }

    private static boolean matchesAny(List<Pattern> patterns, String path) {
        for (Pattern p : patterns) {
            if (p.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    private static List<Pattern> compile(List<String> regexes) {
        List<Pattern> out = new ArrayList<>(regexes.size());
        for (String r : regexes) {
            out.add(Pattern.compile(r));
        }
        return out;
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }
}
