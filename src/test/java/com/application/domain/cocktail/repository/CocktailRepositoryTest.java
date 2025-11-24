package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.Cocktail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CocktailRepositoryTest {

    @Autowired
    private CocktailRepository cocktailRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("동시에 100명이 추천 버튼을 눌러도 카운트는 정확하게 100이 증가해야 한다")
    void concurrencyTest() throws InterruptedException {
        // 1. 트랜잭션 템플릿 준비
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // 2. 데이터 준비 (초기값 0)
        Cocktail cocktail = Cocktail.builder()
                .cocktailKR("모히또")
                .cocktailEN("Mojito")
                .maxAlcohol(10).minAlcohol(5)
                .build();

        // ★ 중요: saveAndFlush로 즉시 DB에 반영하여 다른 스레드가 볼 수 있게 함
        cocktailRepository.saveAndFlush(cocktail);
        Long cocktailId = cocktail.getId();

        int threadCount = 100;
        // ★ 커넥션 풀 고갈 방지를 위해 스레드 수를 HikariCP 기본값(10)에 맞추거나 설정을 늘려야 함.
        // 테스트 안정성을 위해 스레드 풀 사이즈를 조금 줄여서 시도 (10~20)
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 에러 캡처용 변수
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // 3. 실행
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    transactionTemplate.execute(status -> {
                        cocktailRepository.incrementRecommend(cocktailId);
                        return null;
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    e.printStackTrace(); // ★ 에러 발생 시 콘솔에 출력! (이게 핵심)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 종료 대기

        // 4. 검증
        Cocktail result = cocktailRepository.findById(cocktailId).orElseThrow();

        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());
        System.out.println("최종 카운트: " + result.getRecommendCount());

        assertThat(result.getRecommendCount()).isEqualTo(100);
    }
}