package com.application.domain.inquiry.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.domain.inquiry.dto.request.InquiryCreateReq;
import com.application.domain.inquiry.dto.response.InquiryCreateRes;
import com.application.domain.inquiry.dto.response.InquiryPageRes;
import com.application.domain.inquiry.dto.response.InquiryRes;
import com.application.domain.inquiry.entity.Inquiry;
import com.application.domain.inquiry.entity.InquiryStatus;
import com.application.domain.inquiry.repository.InquiryRepository;
import com.application.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    @Transactional
    public InquiryCreateRes create(InquiryCreateReq req, Member memberOrNull, String deviceHeader) {
        String deviceNumber = (req.getDeviceNumber() != null && !req.getDeviceNumber().isBlank())
                ? req.getDeviceNumber()
                : deviceHeader;

        if (memberOrNull == null && (deviceNumber == null || deviceNumber.isBlank())) {
            throw new CustomApiException("비로그인 사용자는 device_number가 필요합니다.");
        }

        Inquiry inquiry = Inquiry.builder()
                .member(memberOrNull)
                .deviceNumber(deviceNumber)
                .title(req.getTitle())
                .content(req.getContent())
                .contact(req.getContact())
                .status(InquiryStatus.NEW)
                .build();

        Inquiry saved = inquiryRepository.save(inquiry);
        log.info("[INQUIRY] Created id={}, memberId={}, device={}",
                saved.getId(),
                memberOrNull != null ? memberOrNull.getId() : null,
                deviceNumber);

        return InquiryCreateRes.builder().id(saved.getId()).build();
    }

    @Transactional(readOnly = true)
    public InquiryPageRes listMine(Long memberId, Pageable pageable) {
        Page<Inquiry> page = inquiryRepository.findByMemberId(memberId, pageable);
        List<InquiryRes> content = page.getContent().stream()
                .map(InquiryRes::fromEntity)
                .toList();
        return InquiryPageRes.fromPage(page, content);
    }

    @Transactional
    public InquiryRes getOneForMemberOrAdmin(Long inquiryId, Long requesterMemberId, boolean isAdmin) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomApiException("문의를 찾을 수 없습니다."));

        if (!isAdmin) {
            Long ownerId = inquiry.getMember() != null ? inquiry.getMember().getId() : null;
            if (ownerId == null || !ownerId.equals(requesterMemberId)) {
                throw new CustomApiException("접근 권한이 없습니다.");
            }
        }

        // Admin/owner 열람 시 NEW → READ 전환
        if (isAdmin && inquiry.getStatus() == InquiryStatus.NEW) {
            inquiry.markRead();
        }
        return InquiryRes.fromEntity(inquiry);
    }

    // ------- Admin helpers (Thymeleaf 페이지용) -------

    @Transactional(readOnly = true)
    public Page<Inquiry> adminList(InquiryStatus status, Pageable pageable) {
        if (status == null) {
            return inquiryRepository.findAll(pageable);
        }
        return inquiryRepository.findAllByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Inquiry adminGet(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new CustomApiException("문의를 찾을 수 없습니다."));
    }

    @Transactional
    public void adminUpdate(Long id, String statusStr, String adminMemo) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new CustomApiException("문의를 찾을 수 없습니다."));

        if (statusStr != null && !statusStr.isBlank()) {
            InquiryStatus status = InquiryStatus.fromString(statusStr)
                    .orElseThrow(() -> new CustomApiException("유효하지 않은 상태값입니다: " + statusStr));
            inquiry.updateStatus(status);
        }

        inquiry.updateAdminMemo(adminMemo);
        inquiryRepository.save(inquiry);
        log.info("[INQUIRY][ADMIN] Updated id={}, status={}, memo_len={}",
                id, inquiry.getStatus(), adminMemo == null ? 0 : adminMemo.length());
    }
}
