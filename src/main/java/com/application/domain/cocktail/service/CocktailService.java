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

    public Map<String, Object> getCocktailTags(CocktailV2Controller.RequestTagType requestTagType) {
        Map<String, Object> result = new HashMap<>();

        if (requestTagType == null || requestTagType.getTagType() == null || requestTagType.getTagType().isEmpty()) {
            for (TagType tagType : TagType.values()) {
                result.put(tagType.name(), getTag(tagType));
            }
            return result;
        }

        try {
            for (String typeStr : requestTagType.getTagType()) {
                TagType tagType = TagType.valueOf(typeStr.trim().toUpperCase());
                result.put(tagType.name(), getTag(tagType));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CustomApiException(e.getMessage());
        }
        return result;
    }

    public List<TagDto> getTag(TagType type) {
        List<Tag> tags = tagRepository.findByType(type);
        List<TagDto> tagDtos = new ArrayList<>();
        for (Tag tag : tags) {
            tagDtos.add(new TagDto(tag.getId(), tag.getName()));
        }
        return tagDtos;
    }

    public List<CocktailDto> getCocktailFindAll(int page, int size) {
        Pageable filterContent = PageRequest.of(page, size);
        Page<Cocktail> pageCocktails = cocktailRepository.findAll(filterContent);
        List<Cocktail> cocktails = pageCocktails.getContent();

        List<CocktailDto> cocktailDtos = new ArrayList<>();
        for (Cocktail cocktail : cocktails) {
            cocktailDtos.add(CocktailMapper.toDto(cocktail));
        }
        return cocktailDtos;
    }

    public Page<CocktailResponseDto> getCocktailsV2(
            CocktailSearchConditionDto condition,
            Pageable pageable,
            CustomOAuth2User user
    ) {
        Page<Cocktail> cocktailPage = cocktailRepository.getCocktails(condition, pageable);
        Set<Long> bookmarkedIds = getBookmarkedIds(user);

        if (condition.korName() != null && !condition.korName().isEmpty()) {
            searchHistoryService.addSearchHistory(user, condition.korName());
        }

        return cocktailPage.map(cocktail ->
                CocktailResponseDto.from(
                        cocktail,
                        bookmarkedIds.contains(cocktail.getId())
                )
        );
    }

    private Set<Long> getBookmarkedIds(CustomOAuth2User user) {
        if (user == null) {
            return Collections.emptySet();
        }

        String credentialId = user.getCredentialId();
        if (credentialId == null) return Collections.emptySet();

        Member member = memberRepository.findByCredentialId(credentialId);
        if (member == null) return Collections.emptySet();

        return bookmarkRepository.findCocktailIdsByMemberId(member.getId());
    }

    public List<CocktailResponseDto> getAllCocktails(CustomOAuth2User user) {
        List<Cocktail> cocktails = cocktailRepository.findAll();
        return convertToCocktailResponseDtos(cocktails, user);
    }

    public List<CocktailResponseDto> getBestCocktails(CustomOAuth2User user) {
        List<Cocktail> cocktails = cocktailRepository.findTop10ByOrderByRecommendCountDesc();
        return convertToCocktailResponseDtos(cocktails, user);
    }

    public List<CocktailResponseDto> getRecentCocktails(CustomOAuth2User user) {
        List<Cocktail> cocktails = cocktailRepository.findTop10ByOrderByUpdatedAtDesc();
        return convertToCocktailResponseDtos(cocktails, user);
    }

    @Transactional(readOnly = true)
    public List<CocktailResponseDto> getSpecificCocktailsV2(List<String> korNameList, CustomOAuth2User user) {
        List<Cocktail> cocktails = cocktailRepository.getSpecificCocktails(korNameList);
        return convertToCocktailResponseDtos(cocktails, user);
    }

    private List<CocktailResponseDto> convertToCocktailResponseDtos(List<Cocktail> cocktails, CustomOAuth2User user) {
        if (user == null) {
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
        final long MIN_ID = 1;
        final long MAX_ID = 105;

        if (cocktailId == null) {
            cocktailId = ThreadLocalRandom.current().nextLong(MIN_ID, MAX_ID + 1);
        }

        Cocktail cocktail = cocktailRepository.findById(cocktailId).orElseThrow(
                () -> new CustomApiException("칵테일이 존재하지 않습니다.")
        );

        if (user == null) {
            return CocktailResponseDto.from(cocktail, null, null, null, false);
        }

        String credentialId = user.getCredentialId();
        if (credentialId == null) {
            log.warn("credentialId is null for user: {}", user.getName());
            return CocktailResponseDto.from(cocktail, null, null, null, false);
        }

        Member member = memberRepository.findByCredentialId(credentialId);
        if (member == null) {
            log.warn("Member not found for credentialId: {}", credentialId);
            return CocktailResponseDto.from(cocktail, null, null, null, false);
        }

        boolean isBookmarked = bookmarkRepository.existsByMemberIdAndCocktailId(member.getId(), cocktailId);

        ReactionType myReaction = reactionRepository.findByMemberIdAndCocktailId(member.getId(), cocktailId)
                .map(CocktailReaction::getReactionType)
                .orElse(null);

        return CocktailResponseDto.from(cocktail, myReaction,
                cocktail.getRecommendCount(), cocktail.getHardCount(), isBookmarked);
    }

    public Cocktail getCocktailV2Entity(Long cocktailId) {
        final long MIN_ID = 1;
        final long MAX_ID = 105;

        if (cocktailId == null) {
            cocktailId = ThreadLocalRandom.current().nextLong(MIN_ID, MAX_ID + 1);
        }

        return cocktailRepository.findById(cocktailId).orElseThrow(
                () -> new CustomApiException("칵테일이 존재하지 않습니다.")
        );
    }

    public CocktailResponseDto getRecommendation(CocktailRecommendationDto dto) {
        List<Cocktail> candidates = cocktailRepository.findRecommendedCocktails(dto);

        if (candidates.isEmpty()) {
            return null;
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(candidates.size());
        Cocktail recommendedCocktail = candidates.get(randomIndex);

        return CocktailResponseDto.from(recommendedCocktail);
    }

    public List<String> getCocktailSuggestions(String searchText) {
        if (searchText == null || searchText.isBlank()) return List.of();
        String q = searchText.trim();
        // 한글/영어 양쪽 contains 매칭 (case-insensitive). 매칭된 쪽 이름 반환.
        List<CocktailRepository.CocktailNamesProjection> projections =
                cocktailRepository.findTop5SuggestionsBilingual(q);

        String lowerQ = q.toLowerCase();
        return projections.stream()
                .map(p -> p.getKorName().toLowerCase().contains(lowerQ) ? p.getKorName() : p.getEngName())
                .toList();
    }

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

    @Transactional(readOnly = true)
    public List<GuideListResponseDto> getCocktailGuideList() {
        return guideRepository.findAll().stream()
                .map(GuideListResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GuideResponseDto getCocktailGuide(Integer part) {
        Guide guide = guideRepository.findByPart(part)
                .orElseThrow(() -> new CustomApiException("해당 파트의 가이드를 찾을 수 없습니다."));

        return GuideResponseDto.from(guide);
    }

    public CocktailDto getCocktailInfo(Long cocktailId) {
        Cocktail cocktail = cocktailRepository.findById(cocktailId).orElseThrow(
                () -> new CustomApiException("칵테일이 존재하지 않습니다.")
        );
        return CocktailMapper.toDto(cocktail);
    }

    public List<CocktailDto> getCocktailSearch(int page, int size,
                                               String searchText, AbvLevel abvLevel,
                                               TasteLevel tasteLevel, List<Season> seasons,
                                               List<Long> tagIds) {

        QCocktail cocktail = QCocktail.cocktail;
        QIngredient ingredient = QIngredient.ingredient;
        QCocktailTag cocktailTag = QCocktailTag.cocktailTag;
        QTag tag = QTag.tag;

        BooleanBuilder builder = new BooleanBuilder();

        if (!searchText.isEmpty()) {
            builder.and(
                    cocktail.cocktailEN.containsIgnoreCase(searchText)
                            .or(cocktail.cocktailKR.containsIgnoreCase(searchText))
                            .or(ingredient.name.containsIgnoreCase(searchText))
            );
        }

        if (abvLevel != null) {
            builder.and(cocktail.abvBand.eq(abvLevel));
        }

        if (tasteLevel != null) {
            builder.and(cocktail.tasteLevel.eq(tasteLevel));
        }

        if (seasons != null && !seasons.isEmpty()) {
            builder.and(cocktail.seasons.any().in(seasons));
        }

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
        for (Cocktail temp : cocktails) {
            dtos.add(CocktailMapper.toDto(temp));
        }

        return dtos;
    }

    public List<String> getRelatedCocktail(String searchText) {
        QCocktail cocktail = QCocktail.cocktail;

        return queryFactory.selectDistinct(cocktail.cocktailKR)
                .from(cocktail)
                .where(cocktail.cocktailKR.containsIgnoreCase(searchText)
                        .or(cocktail.cocktailEN.containsIgnoreCase(searchText)))
                .limit(5)
                .fetch();
    }

    public List<CocktailDto> getPersonalCocktail(CocktailV2Controller.CocktailFilter filter) {
        List<Cocktail> cocktails = cocktailTagRepository.findByTagIds(filter.getTagIds());

        if (cocktails.isEmpty()) {
            cocktails = cocktailRepository.findAll();
        }

        List<Cocktail> filtered = cocktails.stream()
                .filter(c -> filter.getAbvBand() == null || c.getAbvBand().equals(filter.getAbvBand()))
                .filter(c -> filter.getTasteLevel() == null || c.getTasteLevel().equals(filter.getTasteLevel()))
                .filter(c -> filter.getSeason() == null || c.getSeasons().isEmpty() ||
                        c.getSeasons().stream().anyMatch(s -> s.equals(filter.getSeason())))
                .toList();

        if (filtered.isEmpty()) {
            filtered = cocktails.stream()
                    .filter(c -> filter.getAbvBand() == null || c.getAbvBand().equals(filter.getAbvBand()))
                    .filter(c -> filter.getTasteLevel() == null || c.getTasteLevel().equals(filter.getTasteLevel()))
                    .toList();
        }

        if (filtered.isEmpty()) {
            filtered = cocktails.stream()
                    .filter(c -> filter.getAbvBand() == null || c.getAbvBand().equals(filter.getAbvBand()))
                    .toList();
        }

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

        ReactionType myFinalReaction;

        if (existingOpt.isEmpty()) {
            createReaction(member, cocktail, targetType);
            myFinalReaction = targetType;
        } else {
            CocktailReaction existing = existingOpt.get();

            if (existing.getReactionType() == targetType) {
                removeReaction(existing, cocktailId, targetType);
                myFinalReaction = null;
            } else {
                ReactionType previousType = existing.getReactionType();

                existing.updateReactionType(targetType);
                entityManager.flush();

                if (previousType == ReactionType.RECOMMEND) {
                    cocktailRepository.decrementRecommend(cocktailId);
                } else {
                    cocktailRepository.decrementHard(cocktailId);
                }

                if (targetType == ReactionType.RECOMMEND) {
                    cocktailRepository.incrementRecommend(cocktailId);
                } else {
                    cocktailRepository.incrementHard(cocktailId);
                }

                myFinalReaction = targetType;
            }
        }

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
        entityManager.flush();

        if (type == ReactionType.RECOMMEND) cocktailRepository.incrementRecommend(cocktail.getId());
        else cocktailRepository.incrementHard(cocktail.getId());
    }

    private void removeReaction(CocktailReaction reaction, Long cocktailId, ReactionType type) {
        reactionRepository.delete(reaction);
        entityManager.flush();

        if (type == ReactionType.RECOMMEND) cocktailRepository.decrementRecommend(cocktailId);
        else cocktailRepository.decrementHard(cocktailId);
    }
}