package com.application.domain.cocktail.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "북마크 토글 결과 응답 DTO")
public class BookmarkToggleResponse {

    @Schema(description = "칵테일 ID", example = "1")
    private Long cocktailId;

    @Schema(description = "북마크 상태 (true: 북마크됨, false: 북마크 취소됨)", example = "true")
    private boolean isBookmarked;
}
