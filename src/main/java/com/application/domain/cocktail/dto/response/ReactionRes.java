package com.application.domain.cocktail.dto.response;

import com.application.domain.cocktail.enums.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "반응 결과 및 최신 카운트 응답 DTO")
public class ReactionRes {

    @Schema(description = "칵테일 ID", example = "1")
    private Long cocktailId;

    @Schema(description = "현재 나의 반응 상태 (null이면 아무것도 안 누름)", example = "RECOMMEND")
    private ReactionType myReaction;

    @Schema(description = "갱신된 추천해요 총 개수", example = "15")
    private Integer recommendCount;

    @Schema(description = "갱신된 어려워요 총 개수", example = "3")
    private Integer hardCount;
}