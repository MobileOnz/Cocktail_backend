package com.application.domain.cocktail.controller;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.exception.custom.CustomApiException;
import com.application.domain.cocktail.dto.response.CocktailResponseDto;
import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.repository.CocktailBookmarkRepository;
import com.application.domain.cocktail.repository.CocktailReactionRepository;
import com.application.domain.cocktail.repository.CocktailRepository;
import com.application.domain.cocktail.service.CocktailService;
import com.application.domain.cocktail.service.SearchHistoryService;
import com.application.domain.member.entity.Member;
import com.application.domain.member.enums.SocialLogin;
import com.application.domain.member.repository.MemberRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * CocktailService 단위 테스트 (Mock 기반)
 * - 실제 DB 연결 없이 로직만 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("칵테일 서비스 단위 테스트")
class CocktailV2ControllerTest {

    @Mock
    private CocktailRepository cocktailRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CocktailBookmarkRepository bookmarkRepository;

    @Mock
    private CocktailReactionRepository reactionRepository;

    @Mock
    private JPAQueryFactory queryFactory;

    @Mock
    private SearchHistoryService searchHistoryService;

    @Mock
    private CustomOAuth2User customOAuth2User;

    @InjectMocks
    private CocktailService cocktailService;

    private Cocktail testCocktail;
    private Member testMember;

    @BeforeEach
    void setUp() {
        // Mock 칵테일 데이터 생성
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

        // Mock 회원 데이터 생성
        testMember = Member.builder()
                .credentialId("test-credential-id")
                .name("테스트유저")
                .nickname("테스터")
                .email("test@example.com")
                .socialLogin(SocialLogin.KAKAO)
                .build();
        testMember.setId(1L);  // 테스트용 ID 설정
    }

    @Test
    @DisplayName("로그인한 사용자가 북마크한 칵테일 상세 조회 시 is_bookmarked=true 반환")
    void getCocktailDetail_WithBookmark_ReturnsIsBookmarkedTrue() {
        // given
        Long cocktailId = 1L;
        String credentialId = "test-credential-id";

        when(cocktailRepository.findById(cocktailId)).thenReturn(Optional.of(testCocktail));
        when(customOAuth2User.getCredentialId()).thenReturn(credentialId);
        when(memberRepository.findByCredentialId(credentialId)).thenReturn(testMember);
        when(bookmarkRepository.existsByMemberIdAndCocktailId(anyLong(), anyLong())).thenReturn(true);
        when(reactionRepository.findByMemberIdAndCocktailId(anyLong(), anyLong())).thenReturn(Optional.empty());

        // when
        CocktailResponseDto result = cocktailService.getCocktailV2(cocktailId, customOAuth2User);

        // then
        assertThat(result).isNotNull();
        assertThat(result.korName()).isEqualTo("마티니");
        assertThat(result.isBookmarked()).isTrue();
    }

    @Test
    @DisplayName("로그인한 사용자가 북마크하지 않은 칵테일 상세 조회 시 is_bookmarked=false 반환")
    void getCocktailDetail_WithoutBookmark_ReturnsIsBookmarkedFalse() {
        // given
        Long cocktailId = 1L;
        String credentialId = "test-credential-id";

        when(cocktailRepository.findById(cocktailId)).thenReturn(Optional.of(testCocktail));
        when(customOAuth2User.getCredentialId()).thenReturn(credentialId);
        when(memberRepository.findByCredentialId(credentialId)).thenReturn(testMember);
        when(bookmarkRepository.existsByMemberIdAndCocktailId(anyLong(), anyLong())).thenReturn(false);
        when(reactionRepository.findByMemberIdAndCocktailId(anyLong(), anyLong())).thenReturn(Optional.empty());

        // when
        CocktailResponseDto result = cocktailService.getCocktailV2(cocktailId, customOAuth2User);

        // then
        assertThat(result).isNotNull();
        assertThat(result.korName()).isEqualTo("마티니");
        assertThat(result.isBookmarked()).isFalse();
    }

    @Test
    @DisplayName("비로그인 사용자가 칵테일 상세 조회 시 is_bookmarked=false 반환")
    void getCocktailDetail_WithoutAuth_ReturnsIsBookmarkedFalse() {
        // given
        Long cocktailId = 1L;

        when(cocktailRepository.findById(cocktailId)).thenReturn(Optional.of(testCocktail));

        // when - user가 null인 경우
        CocktailResponseDto result = cocktailService.getCocktailV2(cocktailId, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.korName()).isEqualTo("마티니");
        assertThat(result.isBookmarked()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 칵테일 조회 시 예외 발생")
    void getCocktailDetail_NotFound_ThrowsException() {
        // given
        Long nonExistentCocktailId = 99999L;

        when(cocktailRepository.findById(nonExistentCocktailId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cocktailService.getCocktailV2(nonExistentCocktailId, null))
                .isInstanceOf(CustomApiException.class)
                .hasMessage("칵테일이 존재하지 않습니다.");
    }
}
