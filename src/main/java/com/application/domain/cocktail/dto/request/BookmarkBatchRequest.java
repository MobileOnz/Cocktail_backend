package com.application.domain.cocktail.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "즐겨찾기 배치 토글 요청 DTO")
public class BookmarkBatchRequest {

    @JsonProperty("cocktail_ids")
    @Schema(description = "토글할 칵테일 ID 리스트", example = "[1, 2, 3, 4, 5]")
    private List<Long> cocktailIds;
}