package com.application.contract;

import com.application.common.auth.config.EndpointAuthorizationPolicy;
import com.application.common.auth.config.EndpointAuthorizationPolicy.Tier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-05(a)(c) — 엔드포인트 인가 계약 고정 + 유령 경로 검출.
 *
 * <p>목적: fail-closed 회귀 방지. 병렬 세션이 새 엔드포인트를 추가했는데 아무도 등급을
 * 판정하지 않으면, 이 테스트가 <b>실패</b>해서 강제로 분류하게 만든다.</p>
 *
 * <p>방식: 클래스패스에서 컨트롤러 매핑을 반사(reflection)로 전수 열거한다(= Spring 이
 * RequestMappingHandlerMapping 을 만들 때 읽는 것과 동일한 애노테이션). 각 경로를
 * {@link EndpointAuthorizationPolicy}로 판정하고, <b>명시적으로 검토된 기대 등급표</b>와
 * 대조한다. 새 경로가 표에 없거나, 판정 등급이 표와 다르면 실패한다.</p>
 *
 * <p>/admin/** 은 별도 SecurityFilterChain(세션+CSRF+ROLE_ADMIN, T-17)이 관장하므로
 * 이 테스트(=JWTFilter 관할 API 체인) 범위에서 제외한다.</p>
 */
class EndpointAuthorizationContractTest {

    /**
     * ★ 명시적 인가 등급표(단일 진실 원천의 "기대값"). 경로는 path variable 을 "1"로 치환한 구체 경로.
     * 신규 엔드포인트가 생기면 여기에 반드시 추가해야 하고(안 하면 테스트 실패),
     * 추가 시 등급을 의식적으로 고르게 된다. PUBLIC/OPTIONAL 은 JWTFilter 화이트리스트와 일치해야 한다.
     */
    private static final Map<String, Tier> EXPECTED = buildExpected();

    private static Map<String, Tier> buildExpected() {
        Map<String, Tier> m = new LinkedHashMap<>();
        // ── PUBLIC ──
        m.put("/api/v2/auth/social-login", Tier.PUBLIC);
        m.put("/api/v2/auth/signup", Tier.PUBLIC);
        m.put("/api/v2/auth/reissue", Tier.PUBLIC);
        m.put("/api/v2/auth/naver/login-url", Tier.PUBLIC);
        m.put("/api/v2/auth/google/login-url", Tier.PUBLIC);
        m.put("/api/v2/auth/kakao/login-url", Tier.PUBLIC);
        m.put("/login/oauth2/code/naver", Tier.PUBLIC);
        m.put("/login/oauth2/code/google", Tier.PUBLIC);
        m.put("/login/oauth2/code/kakao", Tier.PUBLIC);
        m.put("/api/v2/cocktails/all", Tier.PUBLIC);
        m.put("/api/v2/cocktails/random", Tier.PUBLIC);
        m.put("/api/v2/cocktails/recommendation", Tier.PUBLIC);
        m.put("/api/v2/cocktails/suggestions", Tier.PUBLIC);
        m.put("/api/v2/cocktails/names", Tier.PUBLIC);
        m.put("/api/v2/cocktails/guide", Tier.PUBLIC);
        m.put("/api/v2/cocktails/guide/list", Tier.PUBLIC);
        m.put("/api/v2/cocktails/1/steps", Tier.PUBLIC);        // T-07
        m.put("/api/v2/cocktails/1/guides", Tier.PUBLIC);       // T-09
        m.put("/api/v2/news", Tier.PUBLIC);
        m.put("/api/v2/news/featured", Tier.PUBLIC);            // T-07/08
        m.put("/api/v2/news/1", Tier.PUBLIC);
        m.put("/api/v2/news/1/read", Tier.PUBLIC);              // T-08 (POST, 조회수)
        // 매거진(V10~V12). 런타임 화이트리스트(EndpointAuthorizationPolicy)에는 이미 PUBLIC 으로
        // 올라가 있었는데 이 표에만 빠져 있어서 계약 테스트가 계속 빨간 상태였다.
        m.put("/api/v2/magazine", Tier.PUBLIC);
        m.put("/api/v2/magazine/tags", Tier.PUBLIC);
        m.put("/api/v2/magazine/1", Tier.PUBLIC);
        m.put("/api/v2/magazine/1/read", Tier.PUBLIC);          // POST, 조회수
        m.put("/api/v2/monitoring/track", Tier.PUBLIC);
        m.put("/api/v2/monitoring/onboarding", Tier.PUBLIC);
        m.put("/api/v2/monitoring/onboarding/status", Tier.PUBLIC);
        // ── OPTIONAL ──
        m.put("/api/v2/main", Tier.OPTIONAL);                   // T-09
        m.put("/api/v2/cocktails", Tier.OPTIONAL);              // POST 목록/검색
        m.put("/api/v2/cocktails/detail", Tier.OPTIONAL);
        m.put("/api/v2/cocktails/best", Tier.OPTIONAL);
        m.put("/api/v2/cocktails/recent", Tier.OPTIONAL);
        m.put("/api/v2/cocktails/refresh", Tier.OPTIONAL);
        m.put("/api/v2/cocktails/beginner", Tier.OPTIONAL);
        m.put("/api/v2/cocktails/intermediate", Tier.OPTIONAL);
        m.put("/api/v2/cocktails/search/history", Tier.OPTIONAL);
        m.put("/api/v2/cocktails/search/history/all", Tier.OPTIONAL);
        m.put("/api/v2/cocktails/search/history/1", Tier.OPTIONAL);
        m.put("/api/v2/bars", Tier.OPTIONAL);                   // T-10 (isVisited 개인화 → OPTIONAL)
        m.put("/api/v2/bars/nearby", Tier.OPTIONAL);
        m.put("/api/v2/bars/1", Tier.OPTIONAL);
        m.put("/api/v2/bars/1/menu", Tier.OPTIONAL);            // 세션헤더로 가격 게이팅
        // ── AUTH_REQUIRED (JWT, 화이트리스트 비등록 = 기본 차단) ──
        m.put("/api/v2/members/get/member", Tier.AUTH_REQUIRED);
        m.put("/api/v2/members/update/member", Tier.AUTH_REQUIRED);
        m.put("/api/v2/members/delete/member", Tier.AUTH_REQUIRED);
        m.put("/api/v2/members/upload/profile", Tier.AUTH_REQUIRED);
        m.put("/api/v2/members/profile", Tier.AUTH_REQUIRED);
        m.put("/api/v2/auth/logout", Tier.AUTH_REQUIRED);
        m.put("/api/v2/cocktails/1/reactions", Tier.AUTH_REQUIRED);
        m.put("/api/v2/cocktails/bookmarks/batch", Tier.AUTH_REQUIRED);
        m.put("/api/v2/cocktails/1/bookmarks", Tier.AUTH_REQUIRED);
        m.put("/api/v2/cocktails/bookmarks", Tier.AUTH_REQUIRED);
        m.put("/api/v2/cocktails/1/made", Tier.AUTH_REQUIRED);  // T-07
        m.put("/api/v2/monitoring/info", Tier.AUTH_REQUIRED);   // F-13
        m.put("/api/v2/bars/1/visit", Tier.AUTH_REQUIRED);      // T-10
        m.put("/api/v2/me/visits", Tier.AUTH_REQUIRED);
        m.put("/api/v2/bars/1/visit-session", Tier.AUTH_REQUIRED);       // T-11/13/14 (s4)
        m.put("/api/v2/bars/1/visit-session/renew", Tier.AUTH_REQUIRED);
        m.put("/api/v2/bars/1/chat/stream", Tier.AUTH_REQUIRED);
        m.put("/api/v2/bars/1/chat/messages", Tier.AUTH_REQUIRED);
        m.put("/api/v2/bars/1/chat/messages/1/report", Tier.AUTH_REQUIRED);
        m.put("/api/v2/bars/1/chat/identities/1/block", Tier.AUTH_REQUIRED);
        return m;
    }

    @Test
    @DisplayName("모든 컨트롤러 엔드포인트가 명시적으로 분류되고, JWTFilter 정책 판정과 일치한다")
    void everyEndpointIsExplicitlyClassified() {
        Set<String> runtime = discoverEndpointConcretePaths();

        // (1) 신규/삭제 엔드포인트 검출: 런타임 경로 집합 == 기대표 key 집합
        Set<String> unclassified = new TreeSet<>(runtime);
        unclassified.removeAll(EXPECTED.keySet());
        Set<String> stale = new TreeSet<>(EXPECTED.keySet());
        stale.removeAll(runtime);

        assertThat(unclassified)
                .as("분류되지 않은 신규 엔드포인트 — EXPECTED 표와 (공개라면) JWTFilter 화이트리스트에 등급을 추가하세요")
                .isEmpty();
        assertThat(stale)
                .as("기대표에 있으나 실제 컨트롤러에 없는 엔드포인트(삭제/이름변경) — EXPECTED 표를 갱신하세요")
                .isEmpty();

        // (2) 정책(=필터) 판정이 기대 등급과 정확히 일치하는지
        for (Map.Entry<String, Tier> e : EXPECTED.entrySet()) {
            Tier actual = EndpointAuthorizationPolicy.classify(e.getKey());
            assertThat(actual)
                    .as("경로 %s 의 인가 등급이 기대(%s)와 다릅니다. JWTFilter 화이트리스트를 확인하세요", e.getKey(), e.getValue())
                    .isEqualTo(e.getValue());
        }

        // (3) /onz 접두사 변형도 동일 판정 (context-path 유무 양쪽)
        for (Map.Entry<String, Tier> e : EXPECTED.entrySet()) {
            Tier withCtx = EndpointAuthorizationPolicy.classify(
                    EndpointAuthorizationPolicy.normalize("/onz" + e.getKey()));
            assertThat(withCtx)
                    .as("/onz%s 의 판정이 %s 와 달라졌습니다(normalize 회귀)", e.getKey(), e.getKey())
                    .isEqualTo(e.getValue());
        }
    }

    @Test
    @DisplayName("유령 경로 없음: 모든 PUBLIC/OPTIONAL 패턴이 실제 컨트롤러 매핑과 1:1")
    void noGhostWhitelistPatterns() {
        Set<String> runtime = discoverEndpointConcretePaths();

        for (String regex : EndpointAuthorizationPolicy.apiPublicPatternStrings()) {
            assertThat(runtime.stream().anyMatch(p -> p.matches(regex)))
                    .as("유령 PUBLIC 패턴: '%s' 에 매칭되는 컨트롤러 매핑이 없습니다(삭제 또는 컨트롤러 추가)", regex)
                    .isTrue();
        }
        for (String regex : EndpointAuthorizationPolicy.optionalPatternStrings()) {
            assertThat(runtime.stream().anyMatch(p -> p.matches(regex)))
                    .as("유령 OPTIONAL 패턴: '%s' 에 매칭되는 컨트롤러 매핑이 없습니다", regex)
                    .isTrue();
        }
    }

    // ── 컨트롤러 매핑 전수 열거 (reflection). /admin/** 은 별도 체인이라 제외. ──
    private Set<String> discoverEndpointConcretePaths() {
        Set<String> paths = new TreeSet<>();
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        scanner.findCandidateComponents("com.application").forEach(bd -> {
            Class<?> clazz;
            try {
                clazz = Class.forName(bd.getBeanClassName());
            } catch (ClassNotFoundException ex) {
                return;
            }
            RequestMapping classRm = AnnotatedElementUtils.findMergedAnnotation(clazz, RequestMapping.class);
            String[] basePaths = (classRm == null || classRm.path().length == 0)
                    ? new String[]{""} : classRm.path();

            for (Method method : clazz.getDeclaredMethods()) {
                RequestMapping rm = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (rm == null) {
                    continue;
                }
                String[] subPaths = rm.path().length == 0 ? new String[]{""} : rm.path();
                for (String base : basePaths) {
                    for (String sub : subPaths) {
                        String full = normalizeTemplate(base, sub);
                        String concrete = full.replaceAll("\\{[^/}]+\\}", "1");
                        if (concrete.startsWith("/admin")) {
                            continue; // 별도 SecurityFilterChain (T-17)
                        }
                        paths.add(concrete);
                    }
                }
            }
        });
        return paths;
    }

    private String normalizeTemplate(String base, String sub) {
        String joined = (base + "/" + sub).replaceAll("/{2,}", "/");
        if (joined.length() > 1 && joined.endsWith("/")) {
            joined = joined.substring(0, joined.length() - 1);
        }
        if (!joined.startsWith("/")) {
            joined = "/" + joined;
        }
        return joined.isEmpty() ? "/" : joined;
    }
}
