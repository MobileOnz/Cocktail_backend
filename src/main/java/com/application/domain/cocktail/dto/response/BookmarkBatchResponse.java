package com.application.domain.cocktail.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "즐겨찾기 배치 토글 결과 응답 DTO")
public class BookmarkBatchResponse {

    @JsonProperty("cocktail_ids")
    @Schema(description = "토글 처리된 칵테일 ID 리스트", example = "[1, 2, 3, 4, 5]")
    private List<Long> cocktailIds;
}
