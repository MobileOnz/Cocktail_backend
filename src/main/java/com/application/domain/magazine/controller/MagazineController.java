package com.application.domain.magazine.controller;

import com.application.common.Constant;
import com.application.common.response.ResponseDto;
import com.application.domain.magazine.dto.MagazineDetail;
import com.application.domain.magazine.dto.MagazineFeedResponse;
import com.application.domain.magazine.service.MagazineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 매거진 콘텐츠 API. 모두 PUBLIC(뉴스와 동일 정책).
 * 응답 봉투 {code,msg,data}, data camelCase. content/refs 는 블록 원본(JSONB) 그대로.
 */
@RestController
@RequestMapping("/api/v2/magazine")
@RequiredArgsConstructor
@Tag(name = "매거진 API", description = "블록형 매거진 콘텐츠(스토리/가이드)")
public class MagazineController {

    private final MagazineService magazineService;

    @Operation(summary = "매거진 목록",
            description = "발행분 최신순. category 필터(ALL=전체). 커서 페이지네이션 — 응답의 nextCursor 를 "
                    + "다음 요청 cursor 로 넘긴다. nextCursor 가 null 이면 마지막 페이지.")
    @GetMapping
    public ResponseEntity<ResponseDto<MagazineFeedResponse>> list(
            @Parameter(description = "카테고리 필터. ALL 이면 전체")
            @RequestParam(required = false, defaultValue = "ALL") String category,
            @Parameter(description = "해시태그 필터. 생략하면 전체")
            @RequestParam(required = false) String tag,
            @Parameter(description = "이전 응답의 nextCursor. 생략하면 첫 페이지")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기(1~50)")
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return new ResponseEntity<>(
                ResponseDto.onSuccess("매거진 목록 조회 성공", magazineService.list(category, tag, cursor, size)),
                HttpStatus.OK);
    }

    @Operation(summary = "매거진 태그 목록", description = "발행분에 실제로 붙어 있는 해시태그를 많이 쓰인 순으로.")
    @GetMapping("/tags")
    public ResponseEntity<ResponseDto<List<String>>> tags() {
        return new ResponseEntity<>(ResponseDto.onSuccess("태그 조회 성공", magazineService.tags()), HttpStatus.OK);
    }

    @Operation(summary = "매거진 상세", description = "id 로 단건 조회. 블록 content 반환.")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<MagazineDetail>> detail(@PathVariable Long id) {
        MagazineDetail data = magazineService.detail(id).orElse(null);
        if (data == null) {
            return new ResponseEntity<>(
                    ResponseDto.onFail(Constant.ERROR_CODE, "매거진 글을 찾을 수 없습니다."), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ResponseDto.onSuccess("매거진 상세 조회 성공", data), HttpStatus.OK);
    }

    @Operation(summary = "조회수 증가", description = "매거진 읽음 처리(조회수 +1).")
    @PostMapping("/{id}/read")
    public ResponseEntity<ResponseDto<Void>> read(@PathVariable Long id) {
        boolean ok = magazineService.markRead(id);
        if (!ok) {
            return new ResponseEntity<>(
                    ResponseDto.onFail(Constant.ERROR_CODE, "매거진 글을 찾을 수 없습니다."), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ResponseDto.onSuccess("조회수 반영됨", null), HttpStatus.OK);
    }
}
