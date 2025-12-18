package com.application.domain.cocktail.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.common.mapper.CocktailMapper;
import com.application.domain.cocktail.controller.CocktailV2Controller;
import com.application.domain.cocktail.dto.CocktailDto;
import com.application.domain.cocktail.dto.TagDto;
import com.application.domain.cocktail.dto.response.ReactionRes;
import com.application.domain.cocktail.dto.request.CocktailSearchConditionDto;
import com.application.domain.cocktail.dto.response.CocktailResponseDto;
import com.application.domain.cocktail.entity.*;
import com.application.domain.cocktail.enums.*;
import com.application.domain.cocktail.repository.CocktailReactionRepository;
import com.application.domain.cocktail.repository.CocktailRepository;
import com.application.domain.cocktail.repository.CocktailTagRepository;
import com.application.domain.cocktail.repository.TagRepository;
import com.application.domain.member.entity.Member;
import com.application.domain.member.repository.MemberRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


@Service
@Slf4j
@RequiredArgsConstructor
public class CocktailService {

    private final CocktailRepository cocktailRepository;
    private final CocktailTagRepository cocktailTagRepository;
    private final TagRepository tagRepository;
    private final JPAQueryFactory queryFactory;
    private final CocktailReactionRepository reactionRepository;
    private final MemberRepository memberRepository;

    /* ----------------------- 조회 ------------------------- */

    //TODO) enum값 (맛단계, 도수단계) 조회값 주기
    //TODO) s3 업로드/다운로드 추가

    /* 태그값 조회 */
    
    public Map<String, Object> getCocktailTags(CocktailV2Controller.RequestTagType requestTagType){
        Map<String, Object> result = new HashMap<>();

        // tagtype지정 없을시, 전체 조회
        if(requestTagType == null || requestTagType.getTagType()  == null || requestTagType.getTagType().isEmpty()){

            for(TagType tagType : TagType.values()){
                result.put(tagType.name(), getTag(tagType));
            }

            return result;
        }

        // 지정된 Type 조회
        try{
            for(String typeStr : requestTagType.getTagType()){
                TagType tagType = TagType.valueOf(typeStr.trim().toUpperCase());
                result.put(tagType.name(), getTag(tagType));
            }
        }catch(Exception e){
            log.error(e.getMessage());
            throw new CustomApiException(e.getMessage());
        }
        return result;
    }
    
    public List<TagDto> getTag(TagType type){
        List<Tag> tags = tagRepository.findByType(type);
        List<TagDto> tagDtos = new ArrayList<>();
        for(Tag tag : tags){
            tagDtos.add(new TagDto(tag.getId(), tag.getName()));
        }

        return tagDtos;
    }


    /* 칵테일 조회 */

    /**
     * <pre>
     *     [v1]칵테일 조회
     * </pre>
     * @param page
     * @param size
     * @return
     */
    public List<CocktailDto> getCocktailFindAll(int page, int size){

        //TODO: page 처리 : sorting 은 따로 하지 않음 ( PageRequest.of(page, size, Sort.by("?").descending()) )
        Pageable filterContent = PageRequest.of(page, size);
        Page<Cocktail> pageCocktails = cocktailRepository.findAll(filterContent);
        List<Cocktail> cocktails =  pageCocktails.getContent();

        List<CocktailDto> cocktailDtos = new ArrayList<>();
        for (Cocktail cocktail : cocktails) {
            cocktailDtos.add(CocktailMapper.toDto(cocktail));
        }
        return cocktailDtos;
    }

    // ==========================================================
    // v2
    // ==========================================================

    /**
     * <pre>
     * 칵테일 전체 조회: 페이징, 검색, 필터링을 적용합니다.
     * </pre>
     * @param condition 검색 및 필터링 조건
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return 조건에 맞는 칵테일 목록과 페이징 메타데이터를 포함한 Page 객체
     */
    public Page<CocktailResponseDto> getCocktailsV2(
            CocktailSearchConditionDto condition,
            Pageable pageable
    ) {
        // [주의] 실제 구현 시, 여기서는 QueryDSL 또는 JPA Specification을 사용하여
        //       condition에 따라 동적으로 쿼리를 생성해야 합니다.

        // 예시: Repository에 정의된 동적 쿼리 메서드를 호출한다고 가정
        Page<Cocktail> cocktailPage = cocktailRepository.getCocktails(condition, pageable);

        // Page<Entity>를 Page<DTO>로 변환
        return cocktailPage.map(CocktailResponseDto::from);
    }

    public CocktailResponseDto getCocktailV2(Long cocktailId) {

        // 랜덤 ID 생성 (1부터 105까지 포함)
        final long MIN_ID = 1;
        final long MAX_ID = 105;

        // 랜덤 조회 api 호출 시 사용됨
        if(cocktailId == null) {
            cocktailId = ThreadLocalRandom.current().nextLong(MIN_ID, MAX_ID + 1);
        }

        // 예시: Repository에 정의된 동적 쿼리 메서드를 호출한다고 가정
        Cocktail cocktail = cocktailRepository.findById(cocktailId).orElseThrow(
                () -> new CustomApiException("칵테일이 존재하지 않습니다.")
        );

        return CocktailResponseDto.from(cocktail);
    }

    /**
     * <pre>
     *     best 10 칵테일 조회
     * </pre>
     * @return
     */
    public List<CocktailResponseDto> getBestCocktails() {

        List<Cocktail> cocktails = cocktailRepository.findTop10ByOrderByRecommendCountDesc();

        return cocktails.stream().map(CocktailResponseDto::from).toList();
    }

    /**
     * <pre>
     *     최근 업데이트된 칵테일 10개 조회
     * </pre>
     * @return
     */
    public List<CocktailResponseDto> getRecentCocktails() {

        List<Cocktail> cocktails = cocktailRepository.findTop10ByOrderByUpdatedAtDesc();

        return cocktails.stream().map(CocktailResponseDto::from).toList();
    }

    /**
     * <pre>
     *     칵테일 연관검색어 조회
     * </pre>
     * @param searchText
     * @return
     */
    public List<String> getCocktailSuggestions(String searchText){

        List<CocktailRepository.CocktailNameProjection> projections =
                cocktailRepository.findTop5ByKorNameStartingWith(searchText);

        return projections.stream()
                // CocktailNameProjection 객체에서 getKorName()을 호출하여 String을 얻음
                .map(CocktailRepository.CocktailNameProjection::getKorName)
                .toList();
    }

    /**
     * <pre>
     * 특정 칵테일 조회 FIXME 주석
     * </pre>
     * @return 조건에 맞는 칵테일 목록과 페이징 메타데이터를 포함한 Page 객체
     */
    public List<CocktailResponseDto> getSpecificCocktailsV2(List<String> korNameList) {

        List<Cocktail> cocktails = cocktailRepository.getSpecificCocktails(korNameList);

        return cocktails.stream()
                // ⭐️ Cocktail 엔티티 하나당 CocktailResponseDto::from 메서드를 호출하여 DTO로 매핑합니다.
                .map(CocktailResponseDto::from)
                // ⭐️ 결과를 List로 수집합니다.
                .toList();
    }

    /**
     * <pre>
     *     모든 칵테일의 한글 이름과 영어 이름을 순서대로 조회
     *     반환 형식: [{id: 1, name: "한글1"}, {id: 2, name: "영어1"}, ...]
     * </pre>
     * @return
     */
    public List<CocktailV2Controller.CocktailNameDto> getAllCocktailNames() {

        List<CocktailRepository.CocktailNamesProjection> projections =
                cocktailRepository.findAllProjectedBy();

        List<CocktailV2Controller.CocktailNameDto> allNames = new ArrayList<>();
        int index = 1;
        for (CocktailRepository.CocktailNamesProjection projection : projections) {
            allNames.add(new CocktailV2Controller.CocktailNameDto(index++, projection.getKorName()));
            allNames.add(new CocktailV2Controller.CocktailNameDto(index++, projection.getEngName()));
        }

        return allNames;
    }

    // ======================================================================================

    public CocktailDto getCocktailInfo(Long cocktailId){
        Cocktail cocktail = cocktailRepository.findById(cocktailId).orElseThrow(
                () -> new CustomApiException("칵테일이 존재하지 않습니다.")
        );
        return CocktailMapper.toDto(cocktail);
    }

    /* 칵테일 검색 기능 */

    // 텍스트 검색
    //TODO : 칵테일명 검색
    //TODO : 재료 검색

    // 태깅 및 선택항목
    //TODO : 도수 필터링 검색
    //TODO : 맛 필터링 검색
    //TODO : 맛 레벨 검색
    //TODO : 분위기 필터링 검색
    //TODO : 베이스 술 검색
    //TODO : glass_Type (ex : 하이볼)
    public List<CocktailDto> getCocktailSearch(int page, int size,
                                   String searchText, AbvLevel abvLevel,
                                   TasteLevel tasteLevel, List<Season> seasons,
                                   List<Long> tagIds){

        QCocktail cocktail = QCocktail.cocktail;
        QIngredient ingredient = QIngredient.ingredient;
        QCocktailTag cocktailTag = QCocktailTag.cocktailTag;
        QTag tag = QTag.tag;


        BooleanBuilder builder = new BooleanBuilder();

        // 칵테일명 검색 + 재료 검색
        if(!searchText.isEmpty()){
            builder.and(
                    cocktail.cocktailEN.containsIgnoreCase(searchText)
                            .or(cocktail.cocktailKR.containsIgnoreCase(searchText))
                            .or(ingredient.name.containsIgnoreCase(searchText))
            );
        }

        //도수 Level 매칭
        if(abvLevel != null){
            builder.and(cocktail.abvBand.eq(abvLevel));
        }

        //맛 Level 매칭
        if(tasteLevel != null){
            builder.and(cocktail.tasteLevel.eq(tasteLevel));
        }


        //계절 매칭 (포함관계)
        if (seasons != null && !seasons.isEmpty()) {
            builder.and(cocktail.seasons.any().in(seasons));
        }

        //태그 매칭 ( 맛, 분위기, 잔 종류, 베이스 술 )
        if (tagIds != null && !tagIds.isEmpty()) {
            builder.and(tag.id.in(tagIds));
        }


        List<Cocktail> cocktails = queryFactory
                .selectDistinct(cocktail)
                .from(cocktail)
                .leftJoin(cocktail.ingredients, ingredient)
                .leftJoin(cocktail.tags, cocktailTag)
                .leftJoin(cocktailTag.tag, tag)
                .where(builder)
                .offset((long) page * size)
                .limit(size)
                .fetch();

        List<CocktailDto> dtos = new ArrayList<>();
        for(Cocktail temp : cocktails){
            dtos.add(CocktailMapper.toDto(temp));
        }

        return dtos;
    }


    //TODO : 연관검색어 (Limit 5) , 현재는 그냥 칵테일 매칭
    public List<String> getRelatedCocktail(String searchText){
        QCocktail cocktail = QCocktail.cocktail;

        return queryFactory.selectDistinct(cocktail.cocktailKR)
                .from(cocktail)
                .where(cocktail.cocktailKR.containsIgnoreCase(searchText)
                        .or(cocktail.cocktailEN.containsIgnoreCase(searchText)))
                .limit(5)
                .fetch();
    }


    /* 추천 시스템 */

    /* 칵테일 필터링 종류 */
    // 태그 종류 : 맛, 분위기
    // 추가 옵션 : 계절, 맛 레벨, 도수 레벨
    public List<CocktailDto> getPersonalCocktail(CocktailV2Controller.CocktailFilter filter) {

        // 1) TAG별 cocktail 추출
        List<Cocktail> cocktails = cocktailTagRepository.findByTagIds(filter.getTagIds());

        // tag 필터링에 안걸렸을경우) 전체 조회
        if (cocktails.isEmpty()) {
            cocktails = cocktailRepository.findAll();
        }

        List<Cocktail> filtered = cocktails.stream()
                .filter(c -> filter.getAbvBand() == null || c.getAbvBand() == filter.getAbvBand())
                .filter(c -> filter.getTasteLevel() == null || c.getTasteLevel() == filter.getTasteLevel())
                .filter(c -> filter.getSeason() == null || c.getSeasons().isEmpty() ||
                        c.getSeasons().stream().anyMatch(s -> s == filter.getSeason()))
                .toList();

        // 계절 필터링 제거
        if (filtered.isEmpty()) {
            filtered = cocktails.stream()
                    .filter(c -> filter.getAbvBand() == null || c.getAbvBand() == filter.getAbvBand())
                    .filter(c -> filter.getTasteLevel() == null || c.getTasteLevel() == filter.getTasteLevel())
                    .toList();
        }

        // 맛 필터링 제거
        if (filtered.isEmpty()) {
            filtered = cocktails.stream()
                    .filter(c -> filter.getAbvBand() == null || c.getAbvBand() == filter.getAbvBand())
                    .toList();
        }


        //도수 필터링 제거
        if (filtered.isEmpty()) {
            filtered = cocktails;
        }

        return filtered.stream()
                .map(CocktailMapper::toDto)
                .limit(5)
                .toList();

    }

    @Transactional
    public ReactionRes toggleReaction(Long memberId, Long cocktailId, ReactionType targetType) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Cocktail cocktail = cocktailRepository.findById(cocktailId)
                .orElseThrow(() -> new IllegalArgumentException("Cocktail not found"));

        Optional<CocktailReaction> existingOpt = reactionRepository.findByMemberIdAndCocktailId(memberId, cocktailId);

        if (existingOpt.isEmpty()) {
            // [CASE 1] 아무것도 안 누른 상태 -> 생성
            createReaction(member, cocktail, targetType);
        } else {
            CocktailReaction existing = existingOpt.get();

            if (existing.getReactionType() == targetType) {
                // [CASE 2] 같은거 또 누름 -> 취소 (삭제)
                removeReaction(existing, cocktailId, targetType);
            } else {
                // [CASE 3] 다른거 누름 (추천 -> 어려워요) -> 스위칭
                // 1. 기존 것 삭제 및 카운트 감소
                removeReaction(existing, cocktailId, existing.getReactionType());
                // 2. 새로운 것 생성 및 카운트 증가
                createReaction(member, cocktail, targetType);
            }
        }

        // 최신 카운트 값을 포함하여 응답 반환
        // (영속성 컨텍스트가 갱신되지 않았을 수 있으므로 다시 조회하거나, 계산된 값을 리턴)
        // 안전하게 다시 조회해서 리턴
        Cocktail updatedCocktail = cocktailRepository.findById(cocktailId).get();

        // 현재 유저의 최종 상태 확인
        ReactionType myFinalReaction = reactionRepository.findByMemberIdAndCocktailId(memberId, cocktailId)
                .map(CocktailReaction::getReactionType)
                .orElse(null);

        return ReactionRes.builder()
                .cocktailId(cocktailId)
                .myReaction(myFinalReaction)
                .recommendCount(updatedCocktail.getRecommendCount())
                .hardCount(updatedCocktail.getHardCount())
                .build();
    }

    @Transactional(readOnly = true)
    public ReactionRes getReactionStatus(Long memberId, Long cocktailId) {
        Cocktail cocktail = cocktailRepository.findById(cocktailId)
                .orElseThrow(() -> new IllegalArgumentException("Cocktail not found"));

        ReactionType myReaction = reactionRepository.findByMemberIdAndCocktailId(memberId, cocktailId)
                .map(CocktailReaction::getReactionType)
                .orElse(null);

        return ReactionRes.builder()
                .cocktailId(cocktailId)
                .myReaction(myReaction)
                .recommendCount(cocktail.getRecommendCount())
                .hardCount(cocktail.getHardCount())
                .build();
    }

    private void createReaction(Member member, Cocktail cocktail, ReactionType type) {
        CocktailReaction reaction = CocktailReaction.builder()
                .member(member)
                .cocktail(cocktail)
                .reactionType(type)
                .build();
        reactionRepository.save(reaction);

        if (type == ReactionType.RECOMMEND) cocktailRepository.incrementRecommend(cocktail.getId());
        else cocktailRepository.incrementHard(cocktail.getId());
    }

    private void removeReaction(CocktailReaction reaction, Long cocktailId, ReactionType type) {
        reactionRepository.delete(reaction);

        if (type == ReactionType.RECOMMEND) cocktailRepository.decrementRecommend(cocktailId);
        else cocktailRepository.decrementHard(cocktailId);
    }
}
