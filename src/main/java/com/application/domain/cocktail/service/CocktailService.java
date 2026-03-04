package com.application.domain.cocktail.service;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.exception.custom.CustomApiException;
import com.application.common.mapper.CocktailMapper;
import com.application.domain.cocktail.controller.CocktailV2Controller;
import com.application.domain.cocktail.dto.CocktailDto;
import com.application.domain.cocktail.dto.TagDto;
import com.application.domain.cocktail.dto.request.CocktailRecommendationDto;
import com.application.domain.cocktail.dto.response.GuideListResponseDto;
import com.application.domain.cocktail.dto.response.GuideResponseDto;
import com.application.domain.cocktail.dto.response.ReactionRes;
import com.application.domain.cocktail.dto.request.CocktailSearchConditionDto;
import com.application.domain.cocktail.dto.response.CocktailResponseDto;
import com.application.domain.cocktail.entity.*;
import com.application.domain.cocktail.entity.guide.Guide;
import com.application.domain.cocktail.enums.*;
import com.application.domain.cocktail.repository.*;
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
import java.util.stream.Collectors;


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
    private final GuideRepository guideRepository;

    private final CocktailBookmarkRepository bookmarkRepository;

    private final SearchHistoryService searchHistoryService;

    private final jakarta.persistence.EntityManager entityManager;

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
            Pageable pageable,
            CustomOAuth2User user
    ) {
        // [주의] 실제 구현 시, 여기서는 QueryDSL 또는 JPA Specification을 사용하여
        //       condition에 따라 동적으로 쿼리를 생성해야 합니다.

        // 예시: Repository에 정의된 동적 쿼리 메서드를 호출한다고 가정
        Page<Cocktail> cocktailPage = cocktailRepository.getCocktails(condition, pageable);

        // 2. 현재 유저가 북마크한 칵테일 ID 목록 조회 (Set으로 최적화)
        // (비로그인 유저라면 빈 Set 반환)
        Set<Long> bookmarkedIds = getBookmarkedIds(user);

        // 검색어가 있다면 최근 검색어 저장
        if(condition.korName() != null && !condition.korName().isEmpty()) {
            searchHistoryService.addSearchHistory(user, condition.korName()); // FIXME
        }

        // Page<Entity>를 Page<DTO>로 변환
//        return cocktailPage.map(CocktailResponseDto::from);

        // 3. Entity -> DTO 변환 (북마크 여부 주입)
        return cocktailPage.map(cocktail ->
                CocktailResponseDto.from(
                        cocktail,
                        bookmarkedIds.contains(cocktail.getId()) // ⭐️ 내 북마크 목록에 있으면 true
                )
        );
    }

    /**
     * [Helper] 로그인한 사용자의 북마크 칵테일 ID 목록 조회
     */
    private Set<Long> getBookmarkedIds(CustomOAuth2User user) {
        if (user == null) {
            return Collections.emptySet();
        }

        String credentialId = user.getCredentialId();
        if (credentialId == null) return Collections.emptySet();

        Member member = memberRepository.findByCredentialId(credentialId);
        if (member == null) return Collections.emptySet();

        // Member ID로 북마크한 칵테일 ID만 조회 (BookmarkRepository에 메서드 필요)
        return bookmarkRepository.findCocktailIdsByMemberId(member.getId());
    }

    /**
     * <pre>
     *     best 10 칵테일 조회
     * </pre>
     * @return
     */
    public List<CocktailResponseDto> getBestCocktails(CustomOAuth2User user) {

        List<Cocktail> cocktails = cocktailRepository.findTop10ByOrderByRecommendCountDesc();

        if(user == null) {
            return cocktails.stream().map(CocktailResponseDto::from).toList();
        }

        String credentialId = user.getCredentialId();
        if (credentialId == null) {
            return cocktails.stream().map(CocktailResponseDto::from).toList();
        }

        Member member = memberRepository.findByCredentialId(credentialId);
        if (member == null) {
            log.warn("Member not found for credentialId: {}", credentialId);
            return cocktails.stream().map(CocktailResponseDto::from).toList();
        }

        return cocktails.stream()
                .map(cocktail -> CocktailResponseDto.from(cocktail, member.getId()))
                .toList();
    }

    /**
     * <pre>
     *     최근 업데이트된 칵테일 10개 조회
     * </pre>
     * @return
     */
    public List<CocktailResponseDto> getRecentCocktails(CustomOAuth2User user) {

        List<Cocktail> cocktails = cocktailRepository.findTop10ByOrderByUpdatedAtDesc();

        if(user == null) {
            return cocktails.stream().map(CocktailResponseDto::from).toList();
        }

        String credentialId = user.getCredentialId();
        if (credentialId == null) {
            return cocktails.stream().map(CocktailResponseDto::from).toList();
        }

        Member member = memberRepository.findByCredentialId(credentialId);
        log.info("[북마크 로그] member: {}", member);
        log.info("[북마크 로그] member.id: {}", member.getId());

        if (member == null) {
            log.warn("Member not found for credentialId: {}", credentialId);
            return cocktails.stream().map(CocktailResponseDto::from).toList();
        }

        return cocktails.stream()
                .map(cocktail -> CocktailResponseDto.from(cocktail, member.getId()))
                .toList();
    }

    /**
     * <pre>
     * 특정 칵테일 조회 FIXME 주석
     * </pre>
     * @return 조건에 맞는 칵테일 목록과 페이징 메타데이터를 포함한 Page 객체
     */
    @Transactional(readOnly = true)
    public List<CocktailResponseDto> getSpecificCocktailsV2(List<String> korNameList, CustomOAuth2User user) {

        List<Cocktail> cocktails = cocktailRepository.getSpecificCocktails(korNameList);

        if(user == null) {
            return cocktails.stream().map(CocktailResponseDto::from).toList();
        }

        String credentialId = user.getCredentialId();

        if (credentialId == null) {
            return cocktails.stream().map(CocktailResponseDto::from).toList();
        }

        Member member = memberRepository.findByCredentialId(credentialId);

        if (member == null) {
            log.warn("Member not found for credentialId: {}", credentialId);
            return cocktails.stream().map(CocktailResponseDto::from).toList();
        }

        return cocktails.stream()
                .map(cocktail -> CocktailResponseDto.from(cocktail, member.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CocktailResponseDto getCocktailV2(Long cocktailId, CustomOAuth2User user) {

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

        // 비로그인 사용자 처리
        if(user == null) {
            return CocktailResponseDto.from(cocktail, null, null, null, false);
        }

        String credentialId = user.getCredentialId();
        log.info("Credential ID: {}", credentialId); // 디버깅용 로그
        if (credentialId == null) {
            log.warn("credentialId is null for user: {}", user.getName());
            return CocktailResponseDto.from(cocktail, null, null, null, false);
        }

        Member member = memberRepository.findByCredentialId(credentialId);
        if (member == null) {
            log.warn("Member not found for credentialId: {}", credentialId);
            return CocktailResponseDto.from(cocktail, null, null, null, false);
        }

        // ⭐️ 북마크 여부를 직접 조회 (LAZY 로딩 문제 해결)
        boolean isBookmarked = bookmarkRepository.existsByMemberIdAndCocktailId(member.getId(), cocktailId);

        // 반응 정보 조회
        ReactionType myReaction = reactionRepository.findByMemberIdAndCocktailId(member.getId(), cocktailId)
                .map(CocktailReaction::getReactionType)
                .orElse(null);

        // ⭐️ 새로운 팩토리 메서드 사용: isBookmarked를 직접 전달
        return CocktailResponseDto.from(cocktail, myReaction,
                cocktail.getRecommendCount(), cocktail.getHardCount(), isBookmarked);
    }

    /**
     * 칵테일 엔티티 조회 (내부용 - 컨트롤러에서 북마크 여부 확인 시 사용)
     * getCocktailV2와 동일한 로직이지만 DTO 대신 엔티티를 반환
     */
    public Cocktail getCocktailV2Entity(Long cocktailId) {
        // 랜덤 ID 생성 (1부터 105까지 포함)
        final long MIN_ID = 1;
        final long MAX_ID = 105;

        // 랜덤 조회 api 호출 시 사용됨
        if(cocktailId == null) {
            cocktailId = ThreadLocalRandom.current().nextLong(MIN_ID, MAX_ID + 1);
        }

        return cocktailRepository.findById(cocktailId).orElseThrow(
                () -> new CustomApiException("칵테일이 존재하지 않습니다.")
        );
    }

    public CocktailResponseDto getRecommendation(CocktailRecommendationDto dto) {

        // 1. 조건에 맞는 모든 후보 칵테일 조회
        List<Cocktail> candidates = cocktailRepository.findRecommendedCocktails(dto);

        // 2. 결과가 없으면 예외 처리 또는 기본 추천 (예: 랜덤)
        if (candidates.isEmpty()) {
            return null; // 오류 응답하지 않도록 수정 (결과가 없다는 팝업 띄운다고 함)
//            throw new CustomApiException("조건에 맞는 칵테일을 찾을 수 없습니다.");
            // 또는 return getCocktailRandom(); // 랜덤 반환
        }

        // 3. 후보군 중에서 랜덤으로 1개 선택
        int randomIndex = ThreadLocalRandom.current().nextInt(candidates.size());
        Cocktail recommendedCocktail = candidates.get(randomIndex);

        // 4. DTO 변환 및 반환
        return CocktailResponseDto.from(recommendedCocktail);

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
    // v2 칵테일 가이드
    // ======================================================================================

    /**
     * <pre>
     *     칵테일 가이드 리스트 조회
     * </pre>
     * @return
     */
    @Transactional(readOnly = true) // 리스트 호출을 위한 트랜잭션
    public List<GuideListResponseDto> getCocktailGuideList() {

        return guideRepository.findAll().stream()
                .map(GuideListResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * <pre>
     *     칵테일 가이드 조회
     * </pre>
     * @param part
     * @return
     */
    @Transactional(readOnly = true) // 리스트 호출을 위한 트랜잭션
    public GuideResponseDto getCocktailGuide(Integer part) {

        Guide guide = guideRepository.findByPart(part)
                .orElseThrow(() -> new CustomApiException("해당 파트의 가이드를 찾을 수 없습니다."));

        // 2. DTO 변환 (이때 guide.getDetails()가 호출)
        return GuideResponseDto.from(guide);
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

        ReactionType myFinalReaction; // 최종 반응 상태 추적

        if (existingOpt.isEmpty()) {
            // [CASE 1] 아무것도 안 누른 상태 -> 생성
            createReaction(member, cocktail, targetType);
            myFinalReaction = targetType;
        } else {
            CocktailReaction existing = existingOpt.get();

            if (existing.getReactionType() == targetType) {
                // [CASE 2] 같은거 또 누름 -> 취소 (삭제)
                removeReaction(existing, cocktailId, targetType);
                myFinalReaction = null;
            } else {
                // [CASE 3] 다른거 누름 (추천 -> 어려워요) -> 스위칭
                ReactionType previousType = existing.getReactionType();

                // 1. 반응 타입 업데이트 (DELETE + INSERT 대신 UPDATE 사용)
                existing.updateReactionType(targetType);
                entityManager.flush(); // @Modifying의 clearAutomatically 전에 먼저 flush

                // 2. 기존 카운트 감소
                if (previousType == ReactionType.RECOMMEND) {
                    cocktailRepository.decrementRecommend(cocktailId);
                } else {
                    cocktailRepository.decrementHard(cocktailId);
                }

                // 3. 새로운 카운트 증가
                if (targetType == ReactionType.RECOMMEND) {
                    cocktailRepository.incrementRecommend(cocktailId);
                } else {
                    cocktailRepository.incrementHard(cocktailId);
                }

                myFinalReaction = targetType;
            }
        }

        // 최신 카운트 값을 포함하여 응답 반환
        Cocktail updatedCocktail = cocktailRepository.findById(cocktailId).get();

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
        entityManager.flush(); // @Modifying 전에 flush

        if (type == ReactionType.RECOMMEND) cocktailRepository.incrementRecommend(cocktail.getId());
        else cocktailRepository.incrementHard(cocktail.getId());
    }

    private void removeReaction(CocktailReaction reaction, Long cocktailId, ReactionType type) {
        reactionRepository.delete(reaction);
        entityManager.flush(); // @Modifying 전에 flush

        if (type == ReactionType.RECOMMEND) cocktailRepository.decrementRecommend(cocktailId);
        else cocktailRepository.decrementHard(cocktailId);
    }
}
