package com.application.domain.admin.controller;

import com.application.common.exception.custom.CustomApiException;
import com.application.common.response.ResponseDto;
import com.application.common.storage.ImageStorage;
import com.application.domain.admin.service.AdminGuideService;
import com.application.domain.cocktail.entity.guide.Guide;
import com.application.domain.cocktail.entity.guide.GuideDetail;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 가이드(칵테일 가이드) 관리 컨트롤러.
 *
 * 뷰 라우트와 AJAX 엔드포인트를 한 클래스에 묶어, AdminInquiryController 와 동일한
 * 구조(뷰 + REST 혼합)로 운영자 동선을 단순화한다.
 *
 * 보안: /admin/** 은 SecurityConfig 의 adminSecurity 체인(form-login + 세션) 으로 보호.
 *      별도 인증 코드 불필요. CSRF 는 admin 체인에서 disable.
 *
 * 응답: AJAX 는 ResponseDto<T> ({code, msg, data}) 사용.
 *      JSON 필드는 snake_case 가 필요한 경우 @JsonProperty 명시.
 *
 * 이미지 업로드/삭제: 환경변수 미주입 상태이므로 503 + code:-1 stub 응답.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminGuideController {

    private final AdminGuideService adminGuideService;
    private final ImageStorage imageStorage;

    /* ====================================================================================
     *                                    VIEW ROUTES
     * ==================================================================================== */

    /** GET /admin/guides — 가이드 목록 화면 */
    @GetMapping("/guides")
    public String listView(Model model) {
        List<Guide> guides = adminGuideService.findAll();
        List<GuideRow> rows = new ArrayList<>();
        for (Guide g : guides) {
            int detailCount = g.getDetails() == null ? 0 : g.getDetails().size();
            rows.add(new GuideRow(g.getPart(), g.getTitle(), g.getImageUrl(), detailCount));
        }
        model.addAttribute("guides", rows);
        model.addAttribute("totalCount", rows.size());
        return "admin/guides";
    }

    /** GET /admin/guide/edit?part=X — 기존 가이드 편집 화면 */
    @GetMapping("/guide/edit")
    public String editView(@RequestParam("part") Integer part, Model model) {
        // 컨트롤러에서는 빈 모델만 넘기고, 실제 데이터는 화면 JS 가 /admin/guide/info 로 가져옴
        model.addAttribute("mode", "edit");
        model.addAttribute("part", part);
        return "admin/guide/edit";
    }

    /** GET /admin/guide/new — 신규 가이드 등록 화면 (편집 화면과 동일 템플릿) */
    @GetMapping("/guide/new")
    public String newView(Model model) {
        model.addAttribute("mode", "new");
        model.addAttribute("part", null);
        return "admin/guide/edit";
    }

    /* ====================================================================================
     *                                    AJAX ENDPOINTS
     * ==================================================================================== */

    /** GET /admin/guide/list — 전체 가이드 목록 JSON (요약, details 미포함) */
    @GetMapping("/guide/list")
    @ResponseBody
    public ResponseEntity<ResponseDto<List<GuideRow>>> ajaxList() {
        List<Guide> guides = adminGuideService.findAll();
        List<GuideRow> rows = new ArrayList<>();
        for (Guide g : guides) {
            int detailCount = g.getDetails() == null ? 0 : g.getDetails().size();
            rows.add(new GuideRow(g.getPart(), g.getTitle(), g.getImageUrl(), detailCount));
        }
        return ResponseEntity.ok(ResponseDto.onSuccess("가이드 목록 조회 성공", rows));
    }

    /** GET /admin/guide/info?part=X — 단일 가이드 + details */
    @GetMapping("/guide/info")
    @ResponseBody
    public ResponseEntity<ResponseDto<GuideInfo>> ajaxInfo(@RequestParam("part") Integer part) {
        Guide g = adminGuideService.findByPart(part);
        List<GuideDetail> details = adminGuideService.findDetailsByPart(part);

        GuideHeader header = new GuideHeader(g.getPart(), g.getTitle(), g.getImageUrl());
        List<DetailRow> detailRows = new ArrayList<>();
        for (GuideDetail d : details) {
            detailRows.add(new DetailRow(d.getId(), d.getDisplayOrder(),
                    d.getSubtitle(), d.getDescription(), d.getImageUrl()));
        }
        return ResponseEntity.ok(ResponseDto.onSuccess("가이드 상세 조회 성공",
                new GuideInfo(header, detailRows)));
    }

    /** POST /admin/guide/save — 생성 또는 업데이트 (guide level only) */
    @PostMapping("/guide/save")
    @ResponseBody
    public ResponseEntity<ResponseDto<GuideHeader>> ajaxSave(@RequestBody GuideSaveReq req) {
        if (req == null) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, "요청 본문이 비어 있습니다."));
        }
        try {
            Guide saved = adminGuideService.save(req.getPart(), req.getTitle(), req.getImageUrl());
            return ResponseEntity.ok(ResponseDto.onSuccess("저장 성공",
                    new GuideHeader(saved.getPart(), saved.getTitle(), saved.getImageUrl())));
        } catch (CustomApiException ex) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, ex.getMessage()));
        } catch (Exception ex) {
            log.error("[admin] guide save failed: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(ResponseDto.onFail(-1, "저장 실패: " + ex.getMessage()));
        }
    }

    /** GET /admin/guide/delete?part=X — 가이드 삭제 (cascade detail 자동 삭제) */
    @GetMapping("/guide/delete")
    @ResponseBody
    public ResponseEntity<ResponseDto<Void>> ajaxDelete(@RequestParam("part") Integer part) {
        try {
            adminGuideService.delete(part);
            return ResponseEntity.ok(ResponseDto.onSuccess("삭제되었습니다.", null));
        } catch (CustomApiException ex) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, ex.getMessage()));
        } catch (Exception ex) {
            log.error("[admin] guide delete failed part={} : {}", part, ex.getMessage(), ex);
            return ResponseEntity.ok(ResponseDto.onFail(-1, "삭제 실패: " + ex.getMessage()));
        }
    }

    /** POST /admin/guide/detail/save — detail 단건 저장 (id 있으면 update) */
    @PostMapping("/guide/detail/save")
    @ResponseBody
    public ResponseEntity<ResponseDto<DetailRow>> ajaxDetailSave(@RequestBody DetailSaveReq req) {
        if (req == null) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, "요청 본문이 비어 있습니다."));
        }
        try {
            GuideDetail saved = adminGuideService.saveDetail(
                    req.getId(), req.getGuidePart(), req.getDisplayOrder(),
                    req.getSubtitle(), req.getDescription(), req.getImageUrl());
            return ResponseEntity.ok(ResponseDto.onSuccess("저장 성공",
                    new DetailRow(saved.getId(), saved.getDisplayOrder(),
                            saved.getSubtitle(), saved.getDescription(), saved.getImageUrl())));
        } catch (CustomApiException ex) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, ex.getMessage()));
        } catch (Exception ex) {
            log.error("[admin] guide detail save failed: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(ResponseDto.onFail(-1, "저장 실패: " + ex.getMessage()));
        }
    }

    /** GET /admin/guide/detail/delete?id=X — detail 단건 삭제 */
    @GetMapping("/guide/detail/delete")
    @ResponseBody
    public ResponseEntity<ResponseDto<Void>> ajaxDetailDelete(@RequestParam("id") Long id) {
        try {
            adminGuideService.deleteDetail(id);
            return ResponseEntity.ok(ResponseDto.onSuccess("삭제되었습니다.", null));
        } catch (CustomApiException ex) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, ex.getMessage()));
        } catch (Exception ex) {
            log.error("[admin] guide detail delete failed id={} : {}", id, ex.getMessage(), ex);
            return ResponseEntity.ok(ResponseDto.onFail(-1, "삭제 실패: " + ex.getMessage()));
        }
    }

    /** POST /admin/guide/detail/reorder — bulk reorder */
    @PostMapping("/guide/detail/reorder")
    @ResponseBody
    public ResponseEntity<ResponseDto<Void>> ajaxDetailReorder(@RequestBody ReorderReq req) {
        if (req == null || req.getGuidePart() == null || req.getOrder() == null) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, "요청 본문이 올바르지 않습니다."));
        }
        try {
            List<long[]> pairs = new ArrayList<>();
            for (ReorderItem item : req.getOrder()) {
                if (item.getId() == null || item.getDisplayOrder() == null) {
                    return ResponseEntity.ok(ResponseDto.onFail(-1,
                            "order 항목에 id/display_order 누락이 있습니다."));
                }
                pairs.add(new long[]{item.getId(), item.getDisplayOrder()});
            }
            adminGuideService.reorderDetails(req.getGuidePart(), pairs);
            return ResponseEntity.ok(ResponseDto.onSuccess("순서 변경 성공", null));
        } catch (CustomApiException ex) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, ex.getMessage()));
        } catch (Exception ex) {
            log.error("[admin] guide detail reorder failed: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(ResponseDto.onFail(-1, "순서 변경 실패: " + ex.getMessage()));
        }
    }

    /* ====================================================================================
     *  이미지 업로드/삭제: ImageStorage 추상화 사용.
     *  로컬 환경(AWS 키 없음) → LocalFileImageStorage, 운영 → S3ImageStorage 자동 선택.
     * ==================================================================================== */

    @PostMapping("/guide/image/upload")
    @ResponseBody
    public ResponseEntity<ResponseDto<String>> uploadGuideImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String url = imageStorage.upload("guides", file);
            return ResponseEntity.ok(ResponseDto.onSuccess("업로드 성공 (" + imageStorage.mode() + ")", url));
        } catch (Exception ex) {
            log.error("[admin] guide image upload failed: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(ResponseDto.onFail(-1, "업로드 실패: " + ex.getMessage()));
        }
    }

    @PostMapping("/guide/image/delete")
    @ResponseBody
    public ResponseEntity<ResponseDto<Boolean>> deleteGuideImage(@RequestBody GuideImageDeleteReq req) {
        try {
            boolean deleted = imageStorage.delete(req.getUrl());
            return ResponseEntity.ok(ResponseDto.onSuccess("삭제 처리 완료 (" + imageStorage.mode() + ")", deleted));
        } catch (Exception ex) {
            log.error("[admin] guide image delete failed: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(ResponseDto.onFail(-1, "삭제 실패: " + ex.getMessage()));
        }
    }

    @Getter @Setter
    public static class GuideImageDeleteReq {
        private String url;
    }

    /* ====================================================================================
     *                                       DTOs
     * ==================================================================================== */

    /** 목록 row */
    @Getter
    @AllArgsConstructor
    public static class GuideRow {
        private final Integer part;
        private final String title;

        @JsonProperty("image_url")
        private final String imageUrl;

        @JsonProperty("detail_count")
        private final Integer detailCount;
    }

    /** /info 응답: header + details */
    @Getter
    @AllArgsConstructor
    public static class GuideInfo {
        @JsonProperty("guide")
        private final GuideHeader guide;

        @JsonProperty("details")
        private final List<DetailRow> details;
    }

    @Getter
    @AllArgsConstructor
    public static class GuideHeader {
        private final Integer part;
        private final String title;

        @JsonProperty("image_url")
        private final String imageUrl;
    }

    @Getter
    @AllArgsConstructor
    public static class DetailRow {
        private final Long id;

        @JsonProperty("display_order")
        private final Integer displayOrder;

        private final String subtitle;
        private final String description;

        @JsonProperty("image_url")
        private final String imageUrl;
    }

    @Getter @Setter
    public static class GuideSaveReq {
        @JsonProperty("part")
        private Integer part;

        @JsonProperty("title")
        private String title;

        @JsonProperty("image_url")
        private String imageUrl;
    }

    @Getter @Setter
    public static class DetailSaveReq {
        @JsonProperty("id")
        private Long id; // null → insert

        @JsonProperty("guide_part")
        private Integer guidePart;

        @JsonProperty("display_order")
        private Integer displayOrder;

        @JsonProperty("subtitle")
        private String subtitle;

        @JsonProperty("description")
        private String description;

        @JsonProperty("image_url")
        private String imageUrl;
    }

    @Getter @Setter
    public static class ReorderReq {
        @JsonProperty("guide_part")
        private Integer guidePart;

        @JsonProperty("order")
        private List<ReorderItem> order;
    }

    @Getter @Setter
    public static class ReorderItem {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("display_order")
        private Integer displayOrder;
    }
}
