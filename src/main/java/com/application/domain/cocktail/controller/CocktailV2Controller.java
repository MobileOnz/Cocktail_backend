package com.application.domain.cocktail.controller;

import com.application.common.response.ResponseDto;
import com.application.domain.cocktail.dto.CocktailDto;
import com.application.domain.cocktail.dto.request.CocktailRecommendationDto;
import com.application.domain.cocktail.dto.request.ReactionReq;
import com.application.domain.cocktail.dto.response.GuideResponseDto;
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

import java.util.ArrayList;
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
    @Operation(
            summary = "칵테일 목록 조회",
            description =
                    "**[칵테일 목록 조회 및 필터링]**\n\n" + // \n\n 으로 단락 구분
                            "이 API는 다양한 검색 조건과 페이징을 지원합니다.<br/>" +
                            "모든 옵션은 nullable이며, 결과는 Page<CocktailResponseDto> 형태로 반환됩니다.\n\n" +
                            "**필터링 옵션:**\n" +
                            "- 이름 (korName/engName): 부분 일치 검색\n" +
                            "- 도수 (abvBand): 레벨 필터링 [WEAK | NORMAL | STRONG]\n" +
                            "- 스타일 (style): [스트롱 | 스탠다드 | 스페셜 | 라이트 | 클래식]\n" +
                            "- 베이스 (base): 보드카, 리큐르, 메즈칼, 코냑, 진, 럼 등\n\n" +
                            "**페이징 및 정렬:**\n" +
                            "- page와 size 파라미터로 페이지네이션을 제어합니다. (기본값 - page: 0, size: 10)\n" +
                            "- sort는 \"기준,오름/내림차순\" 형식으로 지정 가능합니다. (기본값 - id,asc)\n" +
                            "  - 기준 - [id | korName | engName] , 오름/내림차순 - [asc | desc]"
    )
    @RequestMapping(path = "", method = RequestMethod.GET)
    public ResponseEntity<ResponseDto<Page<CocktailResponseDto>>> getCocktails(
            @Parameter(description = "검색 및 필터링 정보 (korName=아&abvBand=WEAK)")
            @ModelAttribute CocktailSearchConditionDto condition,
            @Parameter(description = "페이징 및 정렬 정보 (page=0&size=10&sort=name,asc)", example = "{\n" +
                    "  \"page\": 0,\n" +
                    "  \"size\": 10,\n" +
                    "  \"sort\": \"korName,asc\"" +
                    "}")
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

    /**
     * <pre>
     *     칵테일 Best 10 조회
     * </pre>
     * @return
     */
    @Operation(summary = "칵테일 Best 10 조회", description = "추천을 많이 받은 칵테일의 정보를 조회합니다.")
    @GetMapping("/best")
    public ResponseEntity<ResponseDto<List<CocktailResponseDto>>> getBestCocktails(
    ){

        List<CocktailResponseDto> cocktails = cocktailService.getBestCocktails();

        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 조회 성공 (v2)", cocktails),
                HttpStatus.OK
        );
    }

    /**
     * <pre>
     *     최근 업데이트된 칵테일 조회
     * </pre>
     * @return
     */
    @Operation(summary = "칵테일 최신순 top 10 조회", description = "최근 업데이트된 칵테일의 정보를 조회합니다.")
    @GetMapping("/recent")
    public ResponseEntity<ResponseDto<List<CocktailResponseDto>>> getRecentCocktails(
    ){

        List<CocktailResponseDto> cocktails = cocktailService.getRecentCocktails();

        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 조회 성공 (v2)", cocktails),
                HttpStatus.OK
        );
    }

    /**
     * <pre>
     *     상큼한 칵테일 조회
     * </pre>
     *
     * @return
     */
    @Operation(summary = "상큼한 칵테일 7종 목록 조회", description = "칵테일 목록을 조회합니다.")
    @RequestMapping(path = "/refresh", method = RequestMethod.GET)
    public ResponseEntity<ResponseDto<List<CocktailResponseDto>>> getRefreshCocktails() {

        List<String> korNameList = List.of(
                "진 바질 스매시", "네이키드 앤 페이머스", "토미스 마가리타", "옐로 버드", "마가리타", "프렌치 마티니", "미셔너리즈 다운폴"
        );

        List<CocktailResponseDto> cocktails = cocktailService.getSpecificCocktailsV2(korNameList);

        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 목록 조회 성공 (v2)", cocktails),
                HttpStatus.OK
        );
    }

    /**
     * <pre>
     *     입문자용 칵테일 조회
     * </pre>
     *
     * @return
     */
    @Operation(summary = "입문자용 칵테일 7종 목록 조회", description = "칵테일 목록을 조회합니다.")
    @RequestMapping(path = "/beginner", method = RequestMethod.GET)
    public ResponseEntity<ResponseDto<List<CocktailResponseDto>>> getBeginnerCocktails() {

        List<String> korNameList = List.of(
                "아페롤 스프리츠", "메리 픽포드", "다크 앤 스토미", "파라다이스", "사이드카", "샴페인 칵테일", "홀시스 넥"
        );

        List<CocktailResponseDto> cocktails = cocktailService.getSpecificCocktailsV2(korNameList);

        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 목록 조회 성공 (v2)", cocktails),
                HttpStatus.OK
        );
    }

    /**
     * <pre>
     *     중급자 이상 칵테일 조회
     * </pre>
     *
     * @return
     */
    @Operation(summary = "중급자 이상 칵테일 7종 목록 조회", description = "칵테일 목록을 조회합니다.")
    @RequestMapping(path = "/intermediate", method = RequestMethod.GET)
    public ResponseEntity<ResponseDto<List<CocktailResponseDto>>> getIntermediateCocktails() {

        List<String> korNameList = List.of(
                "스팅어", "페이퍼 플레인", "진 바질 스매시", "네이키드 앤 페이머스", "알렉산더", "일레갈", "IBA 티키"
        );

        List<CocktailResponseDto> cocktails = cocktailService.getSpecificCocktailsV2(korNameList);

        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 목록 조회 성공 (v2)", cocktails),
                HttpStatus.OK
        );
    }

    /**
     * <pre>
     *     칵테일 상세 조회
     * </pre>
     * @param cocktailId
     * @return
     */
    @Operation(summary = "칵테일 상세 조회", description = "칵테일 상세 정보를 조회합니다.")
    @GetMapping("/detail")
    public ResponseEntity<ResponseDto<CocktailResponseDto>> getCocktail(
            @Parameter(example = "1")
            @RequestParam Long cocktailId
    ){

        CocktailResponseDto cocktail = cocktailService.getCocktailV2(cocktailId);

        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 조회 성공 (v2)", cocktail),
                HttpStatus.OK
        );
    }

    /**
     * <pre>
     *     칵테일 랜덤 조회
     * </pre>
     * @return
     */
    @Operation(summary = "칵테일 랜덤 조회", description = "ID 1부터 105 사이에서 무작위 칵테일 1개의 상세 정보를 조회합니다.")
    @GetMapping("/random")
    public ResponseEntity<ResponseDto<CocktailResponseDto>> getCocktailRandom(
    ){

        CocktailResponseDto cocktail = cocktailService.getCocktailV2(null);

        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 조회 성공 (v2)", cocktail),
                HttpStatus.OK
        );
    }

    /**
     * <pre>
     *     칵테일 맞춤추천
     * </pre>
     * @return
     */
    @Operation(summary = "칵테일 맞춤추천", description = "조건에 부합하는 칵테일을 조회합니다.")
    @GetMapping("/recommendation")
    public ResponseEntity<ResponseDto<CocktailResponseDto>> getRecommendation(
            @Parameter(description = "질문에 대한 응답")
            @ModelAttribute CocktailRecommendationDto dto
    ){

        CocktailResponseDto cocktail = cocktailService.getRecommendation(dto);

        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 조회 성공 (v2)", cocktail),
                HttpStatus.OK
        );
    }

    @Operation(summary = "칵테일 연관검색어 v2", description = "searchText에 따라 칵테일 이름을 조회합니다.")
    @GetMapping("/suggestions")
    public ResponseEntity<ResponseDto<List<String>>> getCocktailSuggestions(
            @Parameter(example = "아")
            @RequestParam String searchText
    ){

        List<String> cocktailSuggestions = cocktailService.getCocktailSuggestions(searchText);

        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 연관검색어 조회 성공 (v2)", cocktailSuggestions),
                HttpStatus.OK
        );
    }

    /**
     * <pre>
     *     모든 칵테일의 이름 조회 (한글, 영어)
     * </pre>
     * @return
     */
    @Operation(summary = "모든 칵테일 이름 조회", description = "DB에 저장된 모든 칵테일의 한글 이름과 영어 이름을 순번과 함께 순서대로 조회. 형식: [{id: 1, name: \"한글1\"}, {id: 2, name: \"영어1\"}, ...]")
    @GetMapping("/names")
    public ResponseEntity<ResponseDto<List<CocktailNameDto>>> getAllCocktailNames(){

        List<CocktailNameDto> allCocktailNames = cocktailService.getAllCocktailNames();

        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 이름 조회 성공 (v2)", allCocktailNames),
                HttpStatus.OK
        );
    }

    @Getter
    @Setter
    public static class CocktailNameDto {
        private int id;
        private String name;

        public CocktailNameDto(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @GetMapping("/guide")
    public ResponseEntity<ResponseDto<GuideResponseDto>> getCocktailGuide(
            @RequestParam Integer part
    ){

        GuideResponseDto guide = cocktailService.getCocktailGuide(part);

        String msg = "칵테일 가이드 Part " + part + " 조회 성공";

        return new ResponseEntity<>(
                ResponseDto.onSuccess(msg, guide),
                HttpStatus.OK
        );
    }

    // -----------------------
    // ↓ 기존 기능

//    @Override
//    @GetMapping("/{cocktailId}/unused")
//    public ResponseEntity<?> getCocktailUnused(@RequestParam Long cocktailId){
//
//        var result = cocktailService.getCocktailInfo(cocktailId);
//        return ResponseEntity.ok(ResponseDto.onSuccess("cocktail info", result));
////        return new ResponseEntity<>(new ResponseDto<>(1, "cocktail info", cocktailService.getCocktailInfo(cocktailId)),HttpStatus.OK);
//    }

//    @Override
//    @GetMapping("/unused")
//    public ResponseEntity<?> getCocktailsUnused(@RequestParam(value = "page", required = false, defaultValue = "0") int page,@RequestParam(value = "size", required = false, defaultValue = "10") int size){
//        return new ResponseEntity<>(new ResponseDto<>(1, "cocktails info", cocktailService.getCocktailFindAll(page, size)), HttpStatus.OK);
//    }

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

//    @Override
//    @PostMapping("/cocktail/search")
//    public ResponseEntity<?> getCocktailSearch(@RequestBody CocktailSearchRequest request){
//        return new ResponseEntity<>(new ResponseDto<>(1, "serach result",
//                cocktailService.getCocktailSearch(
//                        request.getPage(),
//                        request.getSize(),
//                        request.getSearchText(),
//                        request.getAbvLevel(),
//                        request.getTasteLevel(),
//                        request.getSeasons(),
//                        request.getTagIds()
//                    )
//                )
//                , HttpStatus.OK);
//    }

//    @Override
//    @GetMapping("/cocktail/pre-search")
//    public ResponseEntity<?> getRelatedCocktail(@RequestParam String searchText){
//        return new ResponseEntity<>(new ResponseDto<>(1, "related Search result", cocktailService.getRelatedCocktail(searchText)), HttpStatus.OK);
//    }

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

//    @Override
//    @PostMapping("/cocktail/personalize")
//    public ResponseEntity<?> getPersonalCocktail(@RequestBody CocktailFilter cocktailFilter){
//        return new ResponseEntity<>(new ResponseDto<>(1, "personalize cocktail result", cocktailService.getPersonalCocktail(cocktailFilter)), HttpStatus.OK);
//    }

    @Getter
    @Setter
    public static class RequestTagType{
        @JsonProperty("tagType")
        private List<String> tagType;
    }

//    @Override
//    @PostMapping("/cocktail/tags")
//    public ResponseEntity<?> getCocktailTags(@RequestBody RequestTagType requestTagType){
//        return new ResponseEntity<>(new ResponseDto<>(1, "tags", cocktailService.getCocktailTags(requestTagType)), HttpStatus.OK);
//    }

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
