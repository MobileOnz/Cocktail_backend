package com.application.domain.cocktail.controller;

import com.application.common.auth.jwt.JWTUtil;
import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.CocktailBookmark;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.repository.CocktailBookmarkRepository;
import com.application.domain.cocktail.repository.CocktailRepository;
import com.application.domain.member.entity.Member;
import com.application.domain.member.enums.Role;
import com.application.domain.member.enums.SocialLogin;
import com.application.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("칵테일 V2 컨트롤러 테스트")
class CocktailV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CocktailRepository cocktailRepository;

    @Autowired
    private CocktailBookmarkRepository bookmarkRepository;

    @Autowired
    private JWTUtil jwtUtil;

    private Member testMember;
    private Cocktail testCocktail;
    private String accessToken;

    @BeforeEach
    void setUp() {
        // 1. 테스트 회원 생성 (ID=1)
        testMember = Member.builder()
                .credentialId("test-credential-id")
                .name("테스트유저")
                .nickname("테스터")
                .email("test@example.com")
                .socialLogin(SocialLogin.KAKAO)
                .profile("https://example.com/profile.jpg")
                .role(Role.USER)
                .ageTerm(true)
                .serviceTerm(true)
                .marketingTerm(false)
                .adTerm(false)
                .build();
        testMember = memberRepository.save(testMember);

        // 2. 테스트 칵테일 생성 (ID=5)
        testCocktail = Cocktail.builder()
                .korName("마티니")
                .engName("Martini")
                .abvBand(AbvLevel.STRONG)
                .maxAlcohol(40)
                .minAlcohol(35)
                .originText("클래식 칵테일")
                .season("사계절")
                .ingredientsText("진, 베르무트")
                .style("클래식")
                .glassType("마티니 글라스")
                .glassImageUrl("https://example.com/glass.jpg")
                .base("진")
                .imageUrl("https://example.com/martini.jpg")
                .moods(new ArrayList<>())
                .flavors(new ArrayList<>())
                .tags(new ArrayList<>())
                .recommendCount(10)
                .hardCount(2)
                .build();
        testCocktail = cocktailRepository.save(testCocktail);

        // 3. 북마크 생성 (user_id=1, cocktail_id=5)
        CocktailBookmark bookmark = CocktailBookmark.builder()
                .member(testMember)
                .cocktail(testCocktail)
                .build();
        bookmarkRepository.save(bookmark);

        // 4. JWT 토큰 생성
        accessToken = jwtUtil.createAccessJwt(
                "test-session-id",
                testMember.getCredentialId(),
                testMember.getRole()
        );
    }

    @Test
    @DisplayName("로그인한 사용자가 북마크한 칵테일 상세 조회 시 is_bookmarked=true 반환")
    void getCocktailDetail_WithBookmark_ReturnsIsBookmarkedTrue() throws Exception {
        // given
        Long cocktailId = testCocktail.getId();

        // when & then
        mockMvc.perform(get("/api/v2/cocktails/detail")
                        .param("cocktailId", String.valueOf(cocktailId))
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("칵테일 조회 성공 (v2)"))
                .andExpect(jsonPath("$.data.id").value(cocktailId))
                .andExpect(jsonPath("$.data.kor_name").value("마티니"))
                .andExpect(jsonPath("$.data.is_bookmarked").value(true)); // ⭐️ 핵심: 북마크 여부 확인
    }

    @Test
    @DisplayName("로그인한 사용자가 북마크하지 않은 칵테일 상세 조회 시 is_bookmarked=false 반환")
    void getCocktailDetail_WithoutBookmark_ReturnsIsBookmarkedFalse() throws Exception {
        // given - 다른 칵테일 생성 (북마크 없음)
        Cocktail otherCocktail = Cocktail.builder()
                .korName("모히또")
                .engName("Mojito")
                .abvBand(AbvLevel.WEAK)
                .maxAlcohol(15)
                .minAlcohol(10)
                .originText("쿠바 칵테일")
                .season("여름")
                .ingredientsText("럼, 민트, 라임")
                .style("라이트")
                .glassType("하이볼 글라스")
                .base("럼")
                .imageUrl("https://example.com/mojito.jpg")
                .moods(new ArrayList<>())
                .flavors(new ArrayList<>())
                .tags(new ArrayList<>())
                .build();
        otherCocktail = cocktailRepository.save(otherCocktail);

        // when & then
        mockMvc.perform(get("/api/v2/cocktails/detail")
                        .param("cocktailId", String.valueOf(otherCocktail.getId()))
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(otherCocktail.getId()))
                .andExpect(jsonPath("$.data.kor_name").value("모히또"))
                .andExpect(jsonPath("$.data.is_bookmarked").value(false)); // ⭐️ 북마크하지 않음
    }

    @Test
    @DisplayName("비로그인 사용자가 칵테일 상세 조회 시 is_bookmarked=false 반환")
    void getCocktailDetail_WithoutAuth_ReturnsIsBookmarkedFalse() throws Exception {
        // given
        Long cocktailId = testCocktail.getId();

        // when & then - Authorization 헤더 없이 요청
        mockMvc.perform(get("/api/v2/cocktails/detail")
                        .param("cocktailId", String.valueOf(cocktailId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(cocktailId))
                .andExpect(jsonPath("$.data.is_bookmarked").value(false)); // ⭐️ 비로그인은 항상 false
    }

    @Test
    @DisplayName("공개 API는 인증 없이 정상 호출된다")
    void publicApi_WithoutAuth_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v2/cocktails/best"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("공개 API는 잘못된 Authorization 헤더가 있어도 익명 요청으로 처리된다")
    void publicApi_WithInvalidAuthorizationHeader_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v2/cocktails/best")
                        .header("Authorization", "Bearer invalid-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("회원 API는 인증 없이 호출할 수 없다")
    void memberApi_WithoutAuth_ReturnsClientError() throws Exception {
        mockMvc.perform(get("/api/v2/members/get/member"))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("최근 검색어 API는 인증 없이 호출할 수 없다")
    void searchHistoryApi_WithoutAuth_ReturnsClientError() throws Exception {
        mockMvc.perform(get("/api/v2/cocktails/search/history"))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("모니터링 온보딩 저장 API는 인증 없이 정상 호출된다")
    void monitoringOnboardingApi_WithoutAuth_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v2/monitoring/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceNumber": "test-device-001",
                                  "gender": "none",
                                  "ageRange": "20_24"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("모니터링 온보딩 저장 API는 잘못된 Authorization 헤더가 있어도 익명 요청으로 처리된다")
    void monitoringOnboardingApi_WithInvalidAuthorizationHeader_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v2/monitoring/onboarding")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceNumber": "test-device-002",
                                  "gender": "none",
                                  "ageRange": "20_24"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("테스트 토큰 발급 API는 인증 없이 호출할 수 없다")
    void testTokenApi_WithoutAuth_ReturnsClientError() throws Exception {
        mockMvc.perform(get("/api/v2/test/auth/token")
                        .param("credentialId", "test-credential-id"))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("존재하지 않는 칵테일 조회 시 예외 발생")
    void getCocktailDetail_NotFound_ThrowsException() throws Exception {
        // given
        Long nonExistentCocktailId = 99999L;

        // when & then
        mockMvc.perform(get("/api/v2/cocktails/detail")
                        .param("cocktailId", String.valueOf(nonExistentCocktailId))
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isBadRequest()) // CustomApiException은 400 BAD_REQUEST 반환
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.msg").value("잘못된 요청"));
    }
}
