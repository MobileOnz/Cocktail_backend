package com.application.domain.main.service;

import com.application.domain.cocktail.dto.response.RecommendationResult;
import com.application.domain.cocktail.service.RecommendationService;
import com.application.domain.main.dto.FeedItem;
import com.application.domain.main.dto.HeroDto;
import com.application.domain.main.dto.MainResponse;
import com.application.domain.member.entity.Member;
import com.application.domain.member.repository.MemberRepository;
import com.application.domain.news.entity.News;
import com.application.domain.news.repository.NewsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 메인 첫 화면 합성 (T-09). hero(개인화 추천) + feed(뉴스/가이드 인터리브)를 왕복 1회로 제공.
 * 인터리브 규칙: 뉴스 {@value #NEWS_PER_GUIDE}개마다 가이드 카드 1개 삽입.
 */
@Service
@RequiredArgsConstructor
public class MainFeedService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 40;
    private static final int NEWS_PER_GUIDE = 4;

    private final RecommendationService recommendationService;
    private final NewsRepository newsRepository;
    private final MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public MainResponse getMain(String credentialId, Long cursor, Integer size) {
        // ── hero ──
        Member member = (credentialId == null) ? null : memberRepository.findByCredentialId(credentialId);
        RecommendationResult rec = (member == null)
                ? recommendationService.recommendForAnonymous()
                : recommendationService.recommendForMember(member.getId());
        HeroDto hero = HeroDto.from(rec);

        // ── feed: 뉴스 페이지 + 가이드 인터리브 ──
        int limit = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        List<News> rows = newsRepository.findFeed(null, cursor, PageRequest.of(0, limit + 1));
        boolean hasNext = rows.size() > limit;
        List<News> page = hasNext ? rows.subList(0, limit) : rows;
        String nextCursor = hasNext ? String.valueOf(page.get(page.size() - 1).getId()) : null;

        List<Object[]> guides = guideCards();
        List<FeedItem> feed = new ArrayList<>();
        int guideIdx = 0;
        int newsSinceGuide = 0;
        for (News n : page) {
            feed.add(FeedItem.news(n));
            newsSinceGuide++;
            if (newsSinceGuide == NEWS_PER_GUIDE && !guides.isEmpty()) {
                Object[] g = guides.get(guideIdx % guides.size());
                feed.add(FeedItem.guide(((Number) g[0]).intValue(), str(g[1]), str(g[2])));
                guideIdx++;
                newsSinceGuide = 0;
            }
        }

        return new MainResponse(hero, feed, nextCursor);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> guideCards() {
        return em.createNativeQuery(
                "SELECT part, title, image_url FROM guide ORDER BY part").getResultList();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
