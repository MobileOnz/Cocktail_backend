package com.application.domain.news.dto;

import java.util.List;

/**
 * 뉴스 피드 응답 (T-08). 계약 §4.1 { items, nextCursor }.
 * nextCursor 는 다음 페이지 조회에 넘길 마지막 항목 id(문자열). 더 없으면 null.
 */
public record NewsFeedResponse(
        List<NewsCard> items,
        String nextCursor
) {}
