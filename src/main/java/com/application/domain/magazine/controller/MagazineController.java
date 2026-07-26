package com.application.domain.magazine.controller;

import com.application.common.Constant;
import com.application.common.response.ResponseDto;
import com.application.domain.magazine.dto.MagazineDetail;
import com.application.domain.magazine.dto.MagazineFeedResponse;
import com.application.domain.magazine.service.MagazineService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "매거진 목록", description = "발행분 최신순. category 필터(ALL=전체). 뉴스 피드와 동일 봉투.")
    @GetMapping
    public ResponseEntity<ResponseDto<MagazineFeedResponse>> list(
            @RequestParam(required = false, defaultValue = "ALL") String category) {
        return new ResponseEntity<>(
                ResponseDto.onSuccess("매거진 목록 조회 성공", magazineService.list(category)), HttpStatus.OK);
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
