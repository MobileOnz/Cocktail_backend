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
public class GuideResponseDto {
    private Integer part;
    private String title;
    private String imageUrl;
    private List<DetailDto> details;

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

    public static GuideResponseDto from(Guide guide) {
        return GuideResponseDto.builder()
                .part(guide.getPart())
                .title(guide.getTitle())
                .imageUrl(guide.getImageUrl())
                // stream으로 변환
                .details(guide.getDetails().stream()
                        .map(DetailDto::from)
                        .toList())
                .build();
    }
}