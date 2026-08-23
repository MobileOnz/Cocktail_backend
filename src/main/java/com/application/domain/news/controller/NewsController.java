package com.application.domain.news.controller;

import com.application.common.Constant;
import com.application.common.response.ResponseDto;
import com.application.domain.news.dto.NewsCard;
import com.application.domain.news.dto.NewsDetail;
import com.application.domain.news.dto.NewsFeedResponse;
import com.application.domain.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * T-08 뉴스 컨트롤러. 모두 PUBLIC(계약 §4.1: 뉴스 조회는 OPT/PUB).
 * 응답 봉투 {code,msg,data}, data 내부 camelCase.
 */
@RestController
@RequestMapping("/api/v2/news")
@RequiredArgsConstructor
@Tag(name = "뉴스 API", description = "칵테일/위스키 뉴스 피드")
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "뉴스 피드", description = "커서 페이징 + category 필터(ALL 이면 전체).")
    @GetMapping
    public ResponseEntity<ResponseDto<NewsFeedResponse>> feed(
            @RequestParam(required = false, defaultValue = "ALL") String category,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        NewsFeedResponse data = newsService.getFeed(category, cursor, size);
        return new ResponseEntity<>(ResponseDto.onSuccess("뉴스 목록 조회 성공", data), HttpStatus.OK);
    }

    @Operation(summary = "featured 뉴스", description = "메인 상단 노출용 featured 목록.")
    @GetMapping("/featured")
    public ResponseEntity<ResponseDto<List<NewsCard>>> featured() {
        return new ResponseEntity<>(ResponseDto.onSuccess("featured 뉴스 조회 성공", newsService.getFeatured()), HttpStatus.OK);
    }

    @Operation(summary = "뉴스 상세", description = "id 로 단건 조회.")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<NewsDetail>> detail(@PathVariable Long id) {
        NewsDetail data = newsService.getDetail(id).orElse(null);
        if (data == null) {
            return new ResponseEntity<>(
                    ResponseDto.onFail(Constant.ERROR_CODE, "뉴스를 찾을 수 없습니다."), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ResponseDto.onSuccess("뉴스 상세 조회 성공", data), HttpStatus.OK);
    }

    @Operation(summary = "조회수 증가", description = "뉴스 읽음 처리(조회수 +1).")
    @PostMapping("/{id}/read")
    public ResponseEntity<ResponseDto<Void>> read(@PathVariable Long id) {
        boolean ok = newsService.markRead(id);
        if (!ok) {
            return new ResponseEntity<>(
                    ResponseDto.onFail(Constant.ERROR_CODE, "뉴스를 찾을 수 없습니다."), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ResponseDto.onSuccess("조회수 반영됨", null), HttpStatus.OK);
    }
}
