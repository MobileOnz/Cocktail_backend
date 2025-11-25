package com.application.domain.cocktail.dto.request;

import com.application.domain.cocktail.enums.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "반응(추천/어려워요) 요청 DTO")
public class ReactionReq {

    @Schema(description = "반응 타입 (RECOMMEND: 추천해요, HARD: 조금 어려워요)", example = "RECOMMEND")
    private ReactionType reactionType;
}