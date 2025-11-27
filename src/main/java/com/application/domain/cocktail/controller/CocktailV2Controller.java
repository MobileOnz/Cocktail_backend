package com.application.domain.cocktail.controller;

import com.application.common.response.ResponseDto;
import com.application.domain.cocktail.dto.request.ReactionReq;
import com.application.domain.cocktail.dto.response.ReactionRes;
import com.application.domain.cocktail.dto.request.CocktailSearchConditionDto;
import com.application.domain.cocktail.dto.response.CocktailResponseDto;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.enums.Season;
import com.application.domain.cocktail.enums.TasteLevel;

import com.application.domain.cocktail.service.CocktailService;
import com.application.domain.member.entity.ParsedMember;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/cocktails")
@RequiredArgsConstructor
@Tag(name = "칵테일 관련 API", description = "칵테일 정보 조회, 수정, 삭제 및 검색 등")
public class CocktailV2Controller implements CocktailV2ControllerDocs{
    private final CocktailService cocktailService;

    // v2 -----------------------

    /**
     * <pre>
     * 칵테일 전체 조회 :: 검색, 필터링, 페이징 포함
     * </pre>
     *
     * URL 예시: /api/cocktails?name=마티니&category=클래식&page=0&size=10&sort=id,desc
     *
     * @param condition 검색/필터링 조건 (쿼리 파라미터로 바인딩)
     * @param pageable 페이징 및 정렬 정보 (Spring이 자동 생성)
     * @return
     */
    @Operation(summary = "칵테일 목록 조회", description = "검색, 필터링, 페이징을 적용하여 칵테일 목록을 조회합니다.")
    @RequestMapping(path = "", method = RequestMethod.GET)
    public ResponseEntity<ResponseDto<Page<CocktailResponseDto>>> getCocktails(
            @Parameter(description = "검색 및 필터링 정보 (korName=아&abvBand=WEAK)")
            @ModelAttribute CocktailSearchConditionDto condition,
            @Parameter(description = "페이징 및 정렬 정보 (page=0&size=10&sort=name,asc)")
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        // 1. Service 호출
        Page<CocktailResponseDto> cocktailPage = cocktailService.getCocktailsV2(condition, pageable);

        // 2. 응답 포장
        // Page<T> 객체 자체가 totalPages, totalElements 등 모든 메타데이터를 담고 있으므로,
        // 이를 ResponseDto의 data에 담아 반환합니다.
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 목록 조회 성공 (v2)", cocktailPage),
                HttpStatus.OK
        );
    }

    // -----------------------
    // ↓ 기존 기능

    @Override
    @GetMapping("/{cocktailId}")
    public ResponseEntity<?> getCocktail(@RequestParam Long cocktailId){

        var result = cocktailService.getCocktailInfo(cocktailId);
        return ResponseEntity.ok(ResponseDto.onSuccess("cocktail info", result));
//        return new ResponseEntity<>(new ResponseDto<>(1, "cocktail info", cocktailService.getCocktailInfo(cocktailId)),HttpStatus.OK);
    }

    @Override
    @GetMapping("/unused")
    public ResponseEntity<?> getCocktailsUnused(@RequestParam(value = "page", required = false, defaultValue = "0") int page,@RequestParam(value = "size", required = false, defaultValue = "10") int size){
        return new ResponseEntity<>(new ResponseDto<>(1, "cocktails info", cocktailService.getCocktailFindAll(page, size)), HttpStatus.OK);
    }

    @Getter
    @Setter
    public static class CocktailSearchRequest{
        private int page = 0;
        private int size = 10;
        @JsonProperty("searchText")
        private String searchText;
        @JsonProperty("abvLevel")
        private AbvLevel abvLevel;
        @JsonProperty("tasteLevel")
        private TasteLevel tasteLevel;
        @JsonProperty("seasons")
        private List<Season> seasons;
        @JsonProperty("tagIds")
        private List<Long> tagIds;
    }

    @Override
    @PostMapping("/cocktail/search")
    public ResponseEntity<?> getCocktailSearch(@RequestBody CocktailSearchRequest request){
        return new ResponseEntity<>(new ResponseDto<>(1, "serach result",
                cocktailService.getCocktailSearch(
                        request.getPage(),
                        request.getSize(),
                        request.getSearchText(),
                        request.getAbvLevel(),
                        request.getTasteLevel(),
                        request.getSeasons(),
                        request.getTagIds()
                    )
                )
                , HttpStatus.OK);
    }

    @Override
    @GetMapping("/cocktail/pre-search")
    public ResponseEntity<?> getRelatedCocktail(@RequestParam String searchText){
        return new ResponseEntity<>(new ResponseDto<>(1, "related Search result", cocktailService.getRelatedCocktail(searchText)), HttpStatus.OK);
    }

    @Getter
    @Setter
    public static class CocktailFilter{
        @JsonProperty("abvLevel")
        private AbvLevel abvBand;
        @JsonProperty("tasteLevel")
        private TasteLevel tasteLevel;
        @JsonProperty("season")
        private Season season;
        @JsonProperty("tagIds")
        private List<Long> tagIds;
    }

    @Override
    @PostMapping("/cocktail/personalize")
    public ResponseEntity<?> getPersonalCocktail(@RequestBody CocktailFilter cocktailFilter){
        return new ResponseEntity<>(new ResponseDto<>(1, "personalize cocktail result", cocktailService.getPersonalCocktail(cocktailFilter)), HttpStatus.OK);
    }

    @Getter
    @Setter
    public static class RequestTagType{
        @JsonProperty("tagType")
        private List<String> tagType;
    }

    @Override
    @PostMapping("/cocktail/tags")
    public ResponseEntity<?> getCocktailTags(@RequestBody RequestTagType requestTagType){
        return new ResponseEntity<>(new ResponseDto<>(1, "tags", cocktailService.getCocktailTags(requestTagType)), HttpStatus.OK);
    }

    @Override
    @GetMapping("/{cocktailId}/reactions")
    public ResponseEntity<ReactionRes> getMyReaction( // todo ReactionDto.Response 뭐임?
                                                      @PathVariable Long cocktailId,
                                                      @AuthenticationPrincipal ParsedMember user
    ) {
        Long memberId = Long.valueOf(user.getCredentialId());

        ReactionRes response = cocktailService.getReactionStatus(memberId, cocktailId);
        return ResponseEntity.ok(response);
    }

    @Override // todo swagger
    @PostMapping("/{cocktailId}/reactions")
    public ResponseEntity<ReactionRes> toggleReaction(
            @PathVariable Long cocktailId,
            @RequestBody ReactionReq request,
            @AuthenticationPrincipal ParsedMember user // JWT Filter에서 넣어준 유저 정보
    ) {
        // ParsedMember에 id가 없다면 credentialId로 조회하는 로직이 필요할 수 있음
        // 여기서는 user 객체에 식별자가 있다고 가정
        Long memberId = Long.valueOf(user.getCredentialId());

        ReactionRes response = cocktailService.toggleReaction(memberId, cocktailId, request.getReactionType());
        return ResponseEntity.ok(response);
    }
}
