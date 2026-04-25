package com.application.domain.inquiry.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Schema(description = "1:1 문의 목록 응답 DTO")
public class InquiryPageRes {

    @JsonProperty("content")
    private final List<InquiryRes> content;

    @JsonProperty("page")
    private final int page;

    @JsonProperty("size")
    private final int size;

    @JsonProperty("total_elements")
    private final long totalElements;

    @JsonProperty("total_pages")
    private final int totalPages;

    @Builder
    public InquiryPageRes(List<InquiryRes> content, int page, int size,
                          long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static InquiryPageRes fromPage(Page<?> page, List<InquiryRes> content) {
        return InquiryPageRes.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
