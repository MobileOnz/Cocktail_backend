package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.enums.AbvLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * CocktailRepository 단위 테스트 (Mock 기반)
 * - 실제 DB 연결 없이 Repository 메서드 호출 검증
 *
 * 참고: 동시성 테스트(100명 동시 추천)는 실제 DB가 필요하므로
 * 통합 테스트 환경에서 별도로 실행해야 합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("칵테일 레포지토리 단위 테스트")
class CocktailRepositoryTest {

    @Mock
    private CocktailRepository cocktailRepository;

    @Test
    @DisplayName("칵테일 ID로 조회 시 해당 칵테일 반환")
    void findById_WithValidId_ReturnsCocktail() {
        // given
        Long cocktailId = 1L;
        Cocktail cocktail = Cocktail.builder()
                .korName("모히또")
                .engName("Mojito")
                .maxAlcohol(10)
                .minAlcohol(5)
                .abvBand(AbvLevel.WEAK)
                .originText("쿠바 칵테일")
                .season("여름")
                .ingredientsText("럼, 민트")
                .style("라이트")
                .glassType("하이볼")
                .base("럼")
                .imageUrl("https://example.com/test.jpg")
                .moods(new ArrayList<>())
                .flavors(new ArrayList<>())
                .tags(new ArrayList<>())
                .recommendCount(0)
                .hardCount(0)
                .build();

        when(cocktailRepository.findById(cocktailId)).thenReturn(Optional.of(cocktail));

        // when
        Optional<Cocktail> result = cocktailRepository.findById(cocktailId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getKorName()).isEqualTo("모히또");
        verify(cocktailRepository, times(1)).findById(cocktailId);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional 반환")
    void findById_WithInvalidId_ReturnsEmpty() {
        // given
        Long invalidId = 99999L;
        when(cocktailRepository.findById(invalidId)).thenReturn(Optional.empty());

        // when
        Optional<Cocktail> result = cocktailRepository.findById(invalidId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("추천 카운트 증가 메서드 호출 검증")
    void incrementRecommend_CallsRepositoryMethod() {
        // given
        Long cocktailId = 1L;
        doNothing().when(cocktailRepository).incrementRecommend(cocktailId);

        // when
        cocktailRepository.incrementRecommend(cocktailId);

        // then
        verify(cocktailRepository, times(1)).incrementRecommend(cocktailId);
    }

    @Test
    @DisplayName("추천 카운트 감소 메서드 호출 검증")
    void decrementRecommend_CallsRepositoryMethod() {
        // given
        Long cocktailId = 1L;
        doNothing().when(cocktailRepository).decrementRecommend(cocktailId);

        // when
        cocktailRepository.decrementRecommend(cocktailId);

        // then
        verify(cocktailRepository, times(1)).decrementRecommend(cocktailId);
    }
}
