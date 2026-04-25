package com.application.domain.inquiry.repository;

import com.application.domain.inquiry.entity.Inquiry;
import com.application.domain.inquiry.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findByMemberId(Long memberId, Pageable pageable);

    Page<Inquiry> findAllByStatus(InquiryStatus status, Pageable pageable);
}
