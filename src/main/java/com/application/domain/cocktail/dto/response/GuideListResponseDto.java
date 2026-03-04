package com.application.domain.cocktail.dto.response;

import com.application.domain.cocktail.entity.guide.Guide;
import com.application.domain.cocktail.entity.guide.GuideDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "칵테일 가이드 응답 DTO")
public class GuideListResponseDto {
    private Integer part;
    private String title;
    private String imageUrl;

    @Getter
    @Builder
    public static class DetailDto {
        private Integer displayOrder;
        private String subtitle;
        private String description;
        private String imageUrl;

        public static DetailDto from(GuideDetail detail) {
            return DetailDto.builder()
                    .displayOrder(detail.getDisplayOrder())
                    .subtitle(detail.getSubtitle())
                    .description(detail.getDescription())
                    .imageUrl(detail.getImageUrl())
                    .build();
        }
    }

    public static GuideListResponseDto from(Guide guide) {
        return GuideListResponseDto.builder()
                .part(guide.getPart())
                .title(guide.getTitle())
                .imageUrl(guide.getImageUrl())
                .build();
    }
}