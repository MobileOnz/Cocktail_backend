package com.application.domain.bar.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.domain.bar.dto.response.VisitPageDto;
import com.application.domain.bar.dto.response.VisitPageDto.BarBriefDto;
import com.application.domain.bar.dto.response.VisitPageDto.VisitItemDto;
import com.application.domain.bar.entity.Bar;
import com.application.domain.bar.entity.BarVisit;
import com.application.domain.bar.repository.BarRepository;
import com.application.domain.bar.repository.BarVisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BarVisitService {

    private static final int MAX_LIMIT = 50;

    private final BarService barService;
    private final BarRepository barRepository;
    private final BarVisitRepository barVisitRepository;

    /** 방문 체크. 이미 있으면 그대로 둔다(멱등). */
    @Transactional
    public boolean check(Long memberId, String slug) {
        Bar bar = barService.getActiveBarOrThrow(slug);
        if (!barVisitRepository.existsByMemberIdAndBarId(memberId, bar.getId())) {
            barVisitRepository.save(BarVisit.of(memberId, bar.getId()));
        }
        return true;
    }

    /** 방문 해제. 없으면 그대로 둔다(멱등). */
    @Transactional
    public boolean uncheck(Long memberId, String slug) {
        Bar bar = barService.getActiveBarOrThrow(slug);
        barVisitRepository.findByMemberIdAndBarId(memberId, bar.getId())
                .ifPresent(barVisitRepository::delete);
        return false;
    }

    /**
     * 방문한 바 목록. id DESC 커서 페이징.
     * FE(VisitedBarsScreen)는 `data = { items, nextCursor }` 를 기대하고 nextCursor===null 로 끝을 판정한다.
     */
    public VisitPageDto myVisits(Long memberId, Long cursor, Integer limit) {
        int size = (limit == null || limit <= 0) ? 20 : Math.min(limit, MAX_LIMIT);

        // 다음 페이지 존재 여부를 알기 위해 한 건 더 읽는다.
        List<BarVisit> rows = barVisitRepository.findPageByMember(memberId, cursor, PageRequest.of(0, size + 1));

        boolean hasNext = rows.size() > size;
        List<BarVisit> page = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;

        Map<Long, Bar> barsById = barRepository.findAllById(
                        page.stream().map(BarVisit::getBarId).distinct().toList())
                .stream().collect(Collectors.toMap(Bar::getId, Function.identity()));

        List<VisitItemDto> items = page.stream()
                .map(v -> {
                    Bar b = barsById.get(v.getBarId());
                    if (b == null) throw new CustomApiException("방문 기록의 바를 찾을 수 없습니다");
                    return new VisitItemDto(v.getId(), v.getCheckedAt(),
                            new BarBriefDto(b.getId(), b.getSlug(), b.getNameKo()));
                })
                .toList();

        return new VisitPageDto(items, nextCursor);
    }
}
