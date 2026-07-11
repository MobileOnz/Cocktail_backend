package com.application.domain.news.service;

import com.application.domain.news.dto.NewsCard;
import com.application.domain.news.dto.NewsCategory;
import com.application.domain.news.dto.NewsDetail;
import com.application.domain.news.dto.NewsFeedResponse;
import com.application.domain.news.entity.News;
import com.application.domain.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NewsService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final int FEATURED_LIMIT = 5;

    private final NewsRepository newsRepository;

    /** 커서 페이징 피드. category=ALL/null 은 전체. */
    @Transactional(readOnly = true)
    public NewsFeedResponse getFeed(String category, Long cursor, Integer size) {
        int limit = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String filter = NewsCategory.normalizeFilter(category);

        // size+1 을 가져와 다음 페이지 존재 여부를 판단
        List<News> rows = newsRepository.findFeed(filter, cursor, PageRequest.of(0, limit + 1));

        boolean hasNext = rows.size() > limit;
        List<News> page = hasNext ? rows.subList(0, limit) : rows;
        String nextCursor = hasNext ? String.valueOf(page.get(page.size() - 1).getId()) : null;

        List<NewsCard> items = page.stream().map(NewsCard::from).toList();
        return new NewsFeedResponse(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public Optional<NewsDetail> getDetail(Long id) {
        return newsRepository.findById(id).map(NewsDetail::from);
    }

    @Transactional(readOnly = true)
    public List<NewsCard> getFeatured() {
        return newsRepository.findByFeaturedTrueOrderByPublishedAtDesc(PageRequest.of(0, FEATURED_LIMIT))
                .stream().map(NewsCard::from).toList();
    }

    /** 조회수 +1. 존재하지 않으면 false. */
    @Transactional
    public boolean markRead(Long id) {
        return newsRepository.incrementViewCount(id) > 0;
    }
}
