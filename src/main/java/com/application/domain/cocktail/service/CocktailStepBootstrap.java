package com.application.domain.cocktail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 부팅 시 제조 단계 초안 채우기.
 *
 * <p>{@link CocktailStepGenerator} 와 어드민 엔드포인트는 진작 있었지만 아무도 호출하지 않아
 * 105종 중 93종의 "만드는 법"이 빈 화면이었다(QA: "눌러도 등록이 안 되어 있다").
 * 배포마다 사람이 어드민에 들어가 버튼을 눌러야 채워지는 구조라면 또 잊힌다 → 부팅 때 채운다.</p>
 *
 * <p>비어 있는 칵테일만 채운다(overwriteAuto=false). 그래서:
 * <ul>
 *   <li>두 번째 부팅부터는 아무 일도 하지 않는다 — 멱등이다.</li>
 *   <li>사람이 쓴 단계(source=MANUAL)도, 이미 만들어 둔 초안(AUTO)도 덮어쓰지 않는다.</li>
 * </ul>
 * 규칙을 고쳐 다시 만들고 싶으면 어드민의 {@code overwriteAuto=true} 를 쓴다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CocktailStepBootstrap implements ApplicationRunner {

    private final CocktailStepGenerator generator;

    /** 끄고 싶을 때를 위한 스위치. 기본은 켜짐 — 비어 있는 화면이 기본값이어선 안 된다. */
    @Value("${cocktail.steps.autofill:true}")
    private boolean autofill;

    @Override
    public void run(ApplicationArguments args) {
        if (!autofill) {
            log.info("[STEP-GEN] cocktail.steps.autofill=false — 초안 생성 건너뜀.");
            return;
        }
        try {
            Map<String, Integer> made = generator.generateMissing(false);
            if (made.isEmpty()) {
                log.info("[STEP-GEN] 채울 칵테일 없음 — 이미 모두 단계를 갖고 있다.");
            } else {
                int steps = made.values().stream().mapToInt(Integer::intValue).sum();
                log.info("[STEP-GEN] {}종에 {}단계 생성.", made.size(), steps);
            }
        } catch (Exception e) {
            // 초안이 없다고 서버가 못 뜰 이유는 없다. 실패는 남기고 기동은 계속한다.
            log.error("[STEP-GEN] 초안 생성 실패 — 기동은 계속한다.", e);
        }
    }
}
