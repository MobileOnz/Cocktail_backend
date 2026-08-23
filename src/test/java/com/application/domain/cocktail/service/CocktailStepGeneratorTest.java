package com.application.domain.cocktail.service;

import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.CocktailStep;
import com.application.domain.cocktail.repository.CocktailRepository;
import com.application.domain.cocktail.repository.CocktailStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 제조 단계 생성기.
 *
 * 여기 있는 케이스는 전부 실제 데이터에서 잘못 나왔던 것들이다.
 * 규칙을 손댈 때 같은 실수가 돌아오는지 잡는 게 목적이다.
 */
@SpringBootTest
@ActiveProfiles("test")
class CocktailStepGeneratorTest {

    @Autowired private CocktailStepGenerator generator;
    @Autowired private CocktailRepository cocktailRepository;
    @Autowired private CocktailStepRepository stepRepository;

    @BeforeEach
    void clean() {
        stepRepository.deleteAll();
        cocktailRepository.deleteAll();
    }

    private Cocktail seed(String name, String ingredients, String glass) {
        Cocktail c = Cocktail.builder()
                .korName(name).engName(name)
                .ingredientsText(ingredients)
                .glassType(glass)
                .minAlcohol(10).maxAlcohol(20)
                .style("스탠다드").season("사계절").base("럼")
                .originText("t").imageUrl("t")
                .recommendCount(0).hardCount(0)
                .build();
        return cocktailRepository.save(c);
    }

    private List<CocktailStep> stepsOf(Cocktail c) {
        return stepRepository.findByCocktailIdOrderByStepOrderAsc(c.getId());
    }

    private String joined(Cocktail c) {
        return String.join(" | ", stepsOf(c).stream().map(CocktailStep::getInstruction).toList());
    }

    @Test
    @DisplayName("주스가 들어가면 셰이크, 전부 스피릿이면 스터로 간다")
    void picksMethodFromIngredients() {
        Cocktail shaken = seed("다이키리", "화이트 럼 50ml, 라임 주스 25ml, 설탕 시럽 15ml", "쿠페");
        Cocktail stirred = seed("네그로니", "진 22.5ml, 캄파리 22.5ml, 스위트 베르무트 22.5ml", "록스");

        generator.generateMissing(false);

        assertThat(joined(shaken)).contains("셰이커");
        assertThat(joined(stirred)).contains("믹싱 글라스").doesNotContain("셰이커");
    }

    @Test
    @DisplayName("'크렘 드 민트'는 리큐르지 으깨는 민트가 아니다")
    void liqueurNamedAfterHerbIsNotMuddled() {
        // 처음엔 '민트'라는 글자만 보고 판정해 105종 중 34종이 머들로 잘못 분류됐다.
        Cocktail stinger = seed("스팅어", "코냑 50ml, 화이트 크렘 드 민트 20ml", "쿠페");

        generator.generateMissing(false);

        assertThat(joined(stinger)).doesNotContain("으깬다").contains("믹싱 글라스");
    }

    @Test
    @DisplayName("수량 없는 민트는 장식이고, 수량 있는 민트만 으깬다")
    void quantitySeparatesGarnishFromMuddle() {
        // 마이타이의 '민트 잎'(장식) 과 모히또의 '민트 잎 6장'(으깸) 을 가르는 유일한 신호가 수량이다.
        Cocktail maiTai = seed("마이타이", "자메이카 럼 30ml, 라임 주스 30ml, 파인애플, 민트 잎, 라임 필", "록스");
        Cocktail mojito = seed("모히또", "화이트 럼 45ml, 라임 주스 20ml, 설탕 2 tsp, 민트 잎 6장, 탄산수", "하이볼");

        generator.generateMissing(false);

        assertThat(joined(maiTai)).doesNotContain("으깬다");
        assertThat(joined(maiTai)).contains("민트 잎, 라임 필로 장식한다.");
        assertThat(joined(mojito)).contains("으깬다");
    }

    @Test
    @DisplayName("재료에 섞인 HTML 태그는 지시문으로 새지 않는다")
    void stripsHtmlFromIngredients() {
        Cocktail c = seed("스파이시", "바닐라 보드카 50ml,<br>레드 칠리 1개, 라임 주스 15ml", "쿠페");

        generator.generateMissing(false);

        assertThat(joined(c)).doesNotContain("<").doesNotContain("br>");
    }

    @Test
    @DisplayName("받침에 맞는 조사를 붙인다")
    void usesCorrectKoreanParticle() {
        Cocktail c = seed("테스트", "화이트 럼 45ml, 라임 주스 20ml, 설탕 2 tsp, 민트 잎 6장", "하이볼");

        generator.generateMissing(false);

        // '민트 잎' 은 받침이 있으므로 '을'
        assertThat(joined(c)).doesNotContain("잎를").doesNotContain("필를");
    }

    @Test
    @DisplayName("사람이 쓴 단계는 덮어쓰지 않는다")
    void neverOverwritesManualSteps() {
        Cocktail c = seed("수동", "진 60ml, 라임 주스 20ml", "쿠페");
        stepRepository.save(CocktailStep.auto(c.getId(), 1, "임시", null, null));
        // AUTO 를 MANUAL 로 승격시킨 상황을 흉내낸다.
        stepRepository.deleteAll();
        stepRepository.saveAndFlush(manual(c.getId()));

        generator.generateMissing(true);

        List<CocktailStep> after = stepsOf(c);
        assertThat(after).hasSize(1);
        assertThat(after.get(0).getInstruction()).isEqualTo("사람이 쓴 단계");
        assertThat(after.get(0).getSource()).isEqualTo("MANUAL");
    }

    /** MANUAL 단계는 어드민 경로로만 들어오므로 테스트에서는 AUTO 로 만든 뒤 source 를 바꿔 흉내낸다. */
    private CocktailStep manual(Long cocktailId) {
        CocktailStep s = CocktailStep.auto(cocktailId, 1, "사람이 쓴 단계", null, null);
        org.springframework.test.util.ReflectionTestUtils.setField(s, "source", "MANUAL");
        return s;
    }

    @Test
    @DisplayName("재료가 없으면 단계를 만들지 않는다")
    void skipsCocktailWithoutIngredients() {
        Cocktail c = seed("빈재료", null, "쿠페");

        generator.generateMissing(false);

        assertThat(stepsOf(c)).isEmpty();
    }
}
