package com.application.domain.admin.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.domain.cocktail.entity.guide.Guide;
import com.application.domain.cocktail.entity.guide.GuideDetail;
import com.application.domain.cocktail.repository.GuideDetailRepository;
import com.application.domain.cocktail.repository.GuideRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 관리자 가이드 CRUD 비즈니스 로직.
 *
 * - guide PK 는 part(int) 이며 직접 입력. 신규 등록 시 중복 검증 필요.
 * - guide_detail UNIQUE(guide_part, display_order) 제약 때문에 reorder 시 두 단계 업데이트.
 *   1) 해당 guide_part 모든 행의 display_order 를 음수 영역으로 일괄 이동
 *   2) 새 순서로 한 행씩 양수 갱신
 *   동일 트랜잭션 안에서 끝내야 무결성 깨지지 않음.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminGuideService {

    private final GuideRepository guideRepository;
    private final GuideDetailRepository guideDetailRepository;
    private final EntityManager entityManager;

    /* ============================ 조회 ============================ */

    public List<Guide> findAll() {
        // part(PK, int) 오름차순 정렬
        List<Guide> all = guideRepository.findAll();
        all.sort((a, b) -> Integer.compare(
                a.getPart() == null ? Integer.MAX_VALUE : a.getPart(),
                b.getPart() == null ? Integer.MAX_VALUE : b.getPart()));
        return all;
    }

    public Guide findByPart(Integer part) {
        return guideRepository.findByPart(part)
                .orElseThrow(() -> new CustomApiException("존재하지 않는 가이드입니다. part=" + part));
    }

    public List<GuideDetail> findDetailsByPart(Integer part) {
        return guideDetailRepository.findByGuide_PartOrderByDisplayOrderAsc(part);
    }

    /* ============================ Guide 자체 ============================ */

    @Transactional
    public Guide save(Integer part, String title, String imageUrl) {
        if (part == null) {
            throw new CustomApiException("part 는 필수입니다.");
        }
        if (title == null || title.isBlank()) {
            throw new CustomApiException("title 은 필수입니다.");
        }

        Optional<Guide> existing = guideRepository.findByPart(part);
        Guide entity;
        if (existing.isPresent()) {
            // 업데이트: getter only 엔티티이므로 직접 필드 갱신을 위해 새 빌더로 교체
            Guide old = existing.get();
            // details 컬렉션을 보존한 채 title / imageUrl 만 변경
            // Guide 는 setter 가 없으므로 detach 후 builder 로 재생성하지 않고,
            // EntityManager.merge 를 위해 새 인스턴스 + 기존 details 리스트로 빌드
            entity = Guide.builder()
                    .part(old.getPart())
                    .title(title)
                    .imageUrl(imageUrl)
                    .details(old.getDetails()) // OneToMany 컬렉션 유지
                    .build();
            return entityManager.merge(entity);
        }

        // 신규
        entity = Guide.builder()
                .part(part)
                .title(title)
                .imageUrl(imageUrl)
                .details(new ArrayList<>())
                .build();
        return guideRepository.save(entity);
    }

    @Transactional
    public void delete(Integer part) {
        Guide g = guideRepository.findByPart(part)
                .orElseThrow(() -> new CustomApiException("존재하지 않는 가이드입니다. part=" + part));
        // CascadeType.ALL + orphanRemoval=true 설정으로 details 가 함께 삭제됨
        guideRepository.delete(g);
    }

    /* ============================ GuideDetail ============================ */

    @Transactional
    public GuideDetail saveDetail(Long id, Integer guidePart, Integer displayOrder,
                                  String subtitle, String description, String imageUrl) {
        if (guidePart == null) throw new CustomApiException("guide_part 는 필수입니다.");
        if (displayOrder == null) throw new CustomApiException("display_order 는 필수입니다.");
        if (description == null || description.isBlank()) {
            throw new CustomApiException("description 은 필수입니다.");
        }

        Guide guide = guideRepository.findByPart(guidePart)
                .orElseThrow(() -> new CustomApiException("부모 가이드가 존재하지 않습니다. part=" + guidePart));

        // 동일 (guide_part, display_order) 충돌 체크: 본인을 제외한 다른 행이 사용 중인지
        Optional<GuideDetail> sameSlot = guideDetailRepository
                .findByGuide_PartAndDisplayOrder(guidePart, displayOrder);
        if (sameSlot.isPresent() && (id == null || !sameSlot.get().getId().equals(id))) {
            throw new CustomApiException(
                    "이미 사용 중인 순서입니다. (guide_part=" + guidePart + ", display_order=" + displayOrder + ")");
        }

        GuideDetail detail;
        if (id != null) {
            detail = guideDetailRepository.findById(id)
                    .orElseThrow(() -> new CustomApiException("존재하지 않는 detail 입니다. id=" + id));
            // 기존 엔티티는 setter 가 없으므로 builder 로 재생성 후 merge
            GuideDetail updated = GuideDetail.builder()
                    .id(detail.getId())
                    .guide(guide)
                    .displayOrder(displayOrder)
                    .subtitle(subtitle)
                    .description(description)
                    .imageUrl(imageUrl)
                    .build();
            return entityManager.merge(updated);
        }

        // 신규
        detail = GuideDetail.builder()
                .guide(guide)
                .displayOrder(displayOrder)
                .subtitle(subtitle)
                .description(description)
                .imageUrl(imageUrl)
                .build();
        return guideDetailRepository.save(detail);
    }

    @Transactional
    public void deleteDetail(Long id) {
        if (!guideDetailRepository.existsById(id)) {
            throw new CustomApiException("존재하지 않는 detail 입니다. id=" + id);
        }
        guideDetailRepository.deleteById(id);
    }

    /**
     * reorder 벌크 업데이트.
     *
     * UNIQUE(guide_part, display_order) 제약 때문에 한 줄씩 단순히 update 하면
     * 중간 단계에서 동일 (part, order) 가 두 행에 존재하는 순간이 생겨 SQLException.
     *
     * 해결: 1) 모든 행의 display_order 를 음수 영역으로 이동(temporary)
     *      2) 받은 order 리스트대로 한 행씩 양수 값 갱신
     *      3) 같은 트랜잭션이므로 외부에서는 항상 일관된 상태만 보임
     */
    @Transactional
    public void reorderDetails(Integer guidePart, List<long[]> idOrderPairs) {
        if (guidePart == null) throw new CustomApiException("guide_part 누락");
        if (idOrderPairs == null || idOrderPairs.isEmpty()) {
            throw new CustomApiException("order 리스트가 비어 있습니다.");
        }

        // 같은 displayOrder 가 두 번 들어왔는지, id 가 해당 part 의 detail 인지 검증
        Set<Integer> seenOrders = new HashSet<>();
        for (long[] pair : idOrderPairs) {
            int order = (int) pair[1];
            if (!seenOrders.add(order)) {
                throw new CustomApiException("동일 display_order 가 중복 지정되었습니다: " + order);
            }
        }

        List<GuideDetail> existing = guideDetailRepository.findByGuide_PartOrderByDisplayOrderAsc(guidePart);
        Set<Long> existingIds = new HashSet<>();
        for (GuideDetail d : existing) existingIds.add(d.getId());

        for (long[] pair : idOrderPairs) {
            if (!existingIds.contains(pair[0])) {
                throw new CustomApiException("해당 가이드에 속하지 않은 detail id: " + pair[0]);
            }
        }

        // 1단계: 음수 영역으로 이동 (영속성 컨텍스트와 동기화 위해 flush+clear)
        entityManager.flush();
        guideDetailRepository.shiftToTemporaryNegative(guidePart);
        entityManager.clear();

        // 2단계: 새 order 적용
        for (long[] pair : idOrderPairs) {
            int updated = guideDetailRepository.updateDisplayOrder(pair[0], guidePart, (int) pair[1]);
            if (updated == 0) {
                throw new CustomApiException("display_order 갱신 실패. id=" + pair[0]);
            }
        }
    }
}
