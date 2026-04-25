package com.application.domain.admin.controller;

import com.application.domain.inquiry.entity.Inquiry;
import com.application.domain.inquiry.entity.InquiryStatus;
import com.application.domain.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자용 1:1 문의 관리 Thymeleaf 컨트롤러.
 *
 * 보안: /admin/** 은 기존 SecurityConfig 의 adminSecurity 체인으로 보호되므로
 *      별도 권한 검사 로직 없이 뷰를 반환한다.
 */
@Controller
@RequestMapping("/admin/inquiries")
@RequiredArgsConstructor
@Slf4j
public class AdminInquiryController {

    private final InquiryService inquiryService;

    /** 목록 페이지 */
    @GetMapping
    public String list(@RequestParam(value = "status", required = false) String statusParam,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "20") int size,
                       Model model) {

        InquiryStatus status = null;
        if (statusParam != null && !statusParam.isBlank() && !"ALL".equalsIgnoreCase(statusParam)) {
            status = InquiryStatus.fromString(statusParam).orElse(null);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Inquiry> result = inquiryService.adminList(status, pageable);

        model.addAttribute("inquiries", result.getContent());
        model.addAttribute("currentStatus", statusParam == null ? "ALL" : statusParam.toUpperCase());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalElements", result.getTotalElements());
        model.addAttribute("size", result.getSize());
        model.addAttribute("statuses", InquiryStatus.values());

        return "admin/inquiries";
    }

    /** 상세 페이지 */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Inquiry inquiry = inquiryService.adminGet(id);
        model.addAttribute("inquiry", inquiry);
        model.addAttribute("statuses", InquiryStatus.values());
        return "admin/inquiry-detail";
    }

    /** 상태/메모 수정 */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam(value = "status", required = false) String status,
                         @RequestParam(value = "adminMemo", required = false) String adminMemo) {
        inquiryService.adminUpdate(id, status, adminMemo);
        return "redirect:/admin/inquiries/" + id;
    }
}
