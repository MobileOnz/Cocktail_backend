package com.application.domain.cocktail.controller;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.response.ResponseDto;
import com.application.domain.cocktail.dto.request.CocktailRecommendationDto;
import com.application.domain.cocktail.dto.request.ReactionReq;
import com.application.domain.cocktail.dto.response.GuideListResponseDto;
import com.application.domain.cocktail.dto.response.GuideResponseDto;
import com.application.domain.cocktail.dto.response.ReactionRes;
import com.application.domain.cocktail.dto.request.CocktailSearchConditionDto;
import com.application.domain.cocktail.dto.response.CocktailResponseDto;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.enums.Season;
import com.application.domain.cocktail.enums.TasteLevel;
import com.application.domain.cocktail.service.CocktailBookmarkService;
import com.application.domain.cocktail.service.CocktailService;
import com.application.domain.member.service.MemberService;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v2/cocktails")
@RequiredArgsConstructor
@Tag(name = "칵테일 관련 API", description = "칵테일 정보 조회, 수정, 삭제 및 검색 등")
public class CocktailV2Controller implements CocktailV2ControllerDocs {
    private final CocktailService cocktailService;
    private final CocktailBookmarkService bookmarkService;
    private final MemberService memberService;

    @Operation(
            summary = "칵테일 목록 조회",
            description =
                    "**[칵테일 목록 조회 및 필터링]**\n\n" +
                            "🔓 **인증 불필요** - 이 API는 JWT 토큰 없이 사용 가능합니다.\n\n" +
                            "이 API는 다양한 검색 조건과 페이징을 지원합니다.<br/>" +
                            "모든 옵션은 nullable이며, 결과는 Page<CocktailResponseDto> 형태로 반환됩니다.\n\n" +
                            "**필터링 옵션:**\n" +
                            "- 이름 (korName/engName): 부분 일치 검색\n" +
                            "- 도수 (abvBand): 레벨 필터링 [WEAK | NORMAL | STRONG]\n" +
                            "- 스타일 (style): [스트롱 | 스탠다드 | 스페셜 | 라이트 | 클래식]\n" +
                            "- 맛 (flavor): FRUIT(\"과일\"),\n BITTER(\"쌉쌀함\"),\n SWEET(\"달콤함\"),\n" +
                            " CREAMY(\"부드러움\"),\n COMPLEX(\"복합적인 맛\"),\n HERBAL_SPICE(\"허브 & 스파이스\"),\n" +
                            " LIGHT_REFRESHING(\"라이트 & 청량함\"),\n STRONG_UNIQUE(\"개성 강한 맛\"),\n ETC_SPECIAL(\"기타 & 특별한 맛\")\n" +
                            "  - 다중선택 가능, 교집합으로 조회\n\n" +
                            "- 베이스 (base): 진, 위스키, 럼, 보드카, 데킬라, 브랜디, 리큐르, 와인," +
                            " 기타(셰리, 그라파, 앙고스투라 비터스, 카샤사, 카샤샤, 메스칼, 피스코, 메즈칼, 코냑)\n" +
                            "  - 다중선택 가능, 교집합으로 조회\n\n" +
                            "**페이징 및 정렬:**\n" +
                            "- page와 size 파라미터로 페이지네이션을 제어합니다. (기본값 - page: 0, size: 10)\n" +
                            "- sort는 \"기준,오름/내림차순\" 형식으로 지정 가능합니다. (기본값 - id,asc)\n" +
                            "  - 기준 - [id | korName | engName] , 오름/내림차순 - [asc | desc]"
    )
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<ResponseDto<Page<CocktailResponseDto>>> getCocktails(
            @Parameter(description = "검색 및 필터링 정보 (korName=아&abvBand=WEAK)")
            @RequestBody CocktailSearchConditionDto condition,
            @Parameter(description = "페이징 및 정렬 정보 (page=0&size=10&sort=name,asc)", example = "{\n" +
                    "  \"page\": 0,\n" +
                    "  \"size\": 10,\n" +
                    "  \"sort\": \"korName,asc\"" +
                    "}")
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        Page<CocktailResponseDto> cocktailPage = cocktailService.getCocktailsV2(condition, pageable, customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 목록 조회 성공 (v2)", cocktailPage),
                HttpStatus.OK
        );
    }

    @Operation(summary = "전체 칵테일 목록 조회", description = "🔓 **인증 불필요** - 모든 칵테일의 정보를 조회합니다 (필터링 및 페이징 없음).")
    @SecurityRequirements
    @GetMapping("/all")
    public ResponseEntity<ResponseDto<List<CocktailResponseDto>>> getAllCocktails(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        List<CocktailResponseDto> cocktails = cocktailService.getAllCocktails(customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("전체 칵테일 목록 조회 성공 (v2)", cocktails),
                HttpStatus.OK
        );
    }

    @Operation(summary = "칵테일 Best 10 조회", description = "🔓 **인증 불필요** - 추천을 많이 받은 칵테일의 정보를 조회합니다.")
    @SecurityRequirements
    @GetMapping("/best")
    public ResponseEntity<ResponseDto<List<CocktailResponseDto>>> getBestCocktails(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        List<CocktailResponseDto> cocktails = cocktailService.getBestCocktails(customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 조회 성공 (v2)", cocktails),
                HttpStatus.OK
        );
    }

    @Operation(summary = "칵테일 최신순 top 10 조회", description = "🔓 **인증 불필요** - 최근 업데이트된 칵테일의 정보를 조회합니다.")
    @SecurityRequirements
    @GetMapping("/recent")
    public ResponseEntity<ResponseDto<List<CocktailResponseDto>>> getRecentCocktails(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        List<CocktailResponseDto> cocktails = cocktailService.getRecentCocktails(customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 조회 성공 (v2)", cocktails),
                HttpStatus.OK
        );
    }

    @Operation(summary = "상큼한 칵테일 7종 목록 조회", description = "🔓 **인증 불필요** - 칵테일 목록을 조회합니다.")
    @SecurityRequirements
    @GetMapping("/refresh")
    public ResponseEntity<ResponseDto<List<CocktailResponseDto>>> getRefreshCocktails(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        List<String> korNameList = List.of(
                "진 바질 스매시", "네이키드 앤 페이머스", "토미스 마가리타", "옐로 버드", "마가리타", "프렌치 마티니", "미셔너리즈 다운폴"
        );
        List<CocktailResponseDto> cocktails = cocktailService.getSpecificCocktailsV2(korNameList, customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 목록 조회 성공 (v2)", cocktails),
                HttpStatus.OK
        );
    }

    @Operation(summary = "입문자용 칵테일 7종 목록 조회", description = "🔓 **인증 불필요** - 칵테일 목록을 조회합니다.")
    @SecurityRequirements
    @GetMapping("/beginner")
    public ResponseEntity<ResponseDto<List<CocktailResponseDto>>> getBeginnerCocktails(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        List<String> korNameList = List.of(
                "아페롤 스프리츠", "메리 픽포드", "다크 앤 스토미", "파라다이스", "사이드카", "샴페인 칵테일", "홀시스 넥"
        );
        List<CocktailResponseDto> cocktails = cocktailService.getSpecificCocktailsV2(korNameList, customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 목록 조회 성공 (v2)", cocktails),
                HttpStatus.OK
        );
    }

    @Operation(summary = "중급자 이상 칵테일 7종 목록 조회", description = "🔓 **인증 불필요** - 칵테일 목록을 조회합니다.")
    @SecurityRequirements
    @GetMapping("/intermediate")
    public ResponseEntity<ResponseDto<List<CocktailResponseDto>>> getIntermediateCocktails(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        List<String> korNameList = List.of(
                "스팅어", "페이퍼 플레인", "진 바질 스매시", "네이키드 앤 페이머스", "알렉산더", "일레갈", "IBA 티키"
        );
        List<CocktailResponseDto> cocktails = cocktailService.getSpecificCocktailsV2(korNameList, customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 목록 조회 성공 (v2)", cocktails),
                HttpStatus.OK
        );
    }

    @Operation(summary = "칵테일 상세 조회", description = "🔓 **인증 불필요** - 칵테일 상세 정보를 조회합니다.")
    @SecurityRequirements
    @GetMapping("/detail")
    public ResponseEntity<ResponseDto<CocktailResponseDto>> getCocktail(
            @Parameter(example = "1")
            @RequestParam Long cocktailId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        CocktailResponseDto cocktail = cocktailService.getCocktailV2(cocktailId, customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 조회 성공 (v2)", cocktail),
                HttpStatus.OK
        );
    }

    @Operation(summary = "칵테일 랜덤 조회", description = "🔓 **인증 불필요** - ID 1부터 105 사이에서 무작위 칵테일 1개의 상세 정보를 조회합니다.")
    @SecurityRequirements
    @GetMapping("/random")
    public ResponseEntity<ResponseDto<CocktailResponseDto>> getCocktailRandom(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        CocktailResponseDto cocktail = cocktailService.getCocktailV2(null, customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 조회 성공 (v2)", cocktail),
                HttpStatus.OK
        );
    }

    @Operation(summary = "칵테일 맞춤추천", description = "🔓 **인증 불필요** - 조건에 부합하는 칵테일을 조회합니다.")
    @SecurityRequirements
    @GetMapping("/recommendation")
    public ResponseEntity<ResponseDto<CocktailResponseDto>> getRecommendation(
            @Parameter(description = "질문에 대한 응답")
            @ModelAttribute CocktailRecommendationDto dto
    ) {
        CocktailResponseDto cocktail = cocktailService.getRecommendation(dto);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 조회 성공 (v2)", cocktail),
                HttpStatus.OK
        );
    }

    @Operation(summary = "칵테일 연관검색어 v2", description = "🔓 **인증 불필요** - searchText에 따라 칵테일 이름을 조회합니다.")
    @SecurityRequirements
    @GetMapping("/suggestions")
    public ResponseEntity<ResponseDto<List<String>>> getCocktailSuggestions(
            @Parameter(example = "아")
            @RequestParam String searchText
    ) {
        List<String> cocktailSuggestions = cocktailService.getCocktailSuggestions(searchText);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 연관검색어 조회 성공 (v2)", cocktailSuggestions),
                HttpStatus.OK
        );
    }

    @Operation(summary = "모든 칵테일 이름 조회", description = "🔓 **인증 불필요** - DB에 저장된 모든 칵테일의 한글 이름과 영어 이름을 순번과 함께 순서대로 조회. 형식: [{id: 1, name: \"한글1\"}, {id: 2, name: \"영어1\"}, ...]")
    @SecurityRequirements
    @GetMapping("/names")
    public ResponseEntity<ResponseDto<List<CocktailNameDto>>> getAllCocktailNames() {
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

    @Getter
    @Setter
    public static class CocktailSearchRequest {
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

    @Getter
    @Setter
    public static class CocktailFilter {
        @JsonProperty("abvLevel")
        private AbvLevel abvBand;
        @JsonProperty("tasteLevel")
        private TasteLevel tasteLevel;
        @JsonProperty("season")
        private Season season;
        @JsonProperty("tagIds")
        private List<Long> tagIds;
    }

    @Getter
    @Setter
    public static class RequestTagType {
        @JsonProperty("tagType")
        private List<String> tagType;
    }

    @Operation(summary = "칵테일 가이드 목록 조회", description = "🔓 **인증 불필요** - 모든 가이드의 파트 번호와 제목 리스트를 조회합니다.")
    @SecurityRequirements
    @GetMapping("/guide/list")
    public ResponseEntity<ResponseDto<List<GuideListResponseDto>>> getGuideList() {
        List<GuideListResponseDto> list = cocktailService.getCocktailGuideList();
        return new ResponseEntity<>(
                ResponseDto.onSuccess("칵테일 가이드 목록 조회 성공", list),
                HttpStatus.OK
        );
    }

    @Operation(summary = "칵테일 가이드 상세 조회",
            description = "🔓 **인증 불필요** - 각 가이드의 상세 정보를 조회합니다.\n\n" +
                    "part * 100 + ep 값을 전달하면 됩니다. (ex : part1 ep2 -> 102 ")
    @SecurityRequirements
    @GetMapping("/guide")
    public ResponseEntity<ResponseDto<GuideResponseDto>> getCocktailGuide(
            @RequestParam Integer part
    ) {
        GuideResponseDto guide = cocktailService.getCocktailGuide(part);
        String msg = "칵테일 가이드 Part " + part + " 조회 성공";
        return new ResponseEntity<>(
                ResponseDto.onSuccess(msg, guide),
                HttpStatus.OK
        );
    }

    @Override
    @Operation(summary = "칵테일 반응 조회", description = "🔒 **인증 필요** - 로그인한 사용자의 해당 칵테일에 대한 반응(좋아요/싫어요) 상태를 조회합니다.")
    @GetMapping("/{cocktailId}/reactions")
    public ResponseEntity<ReactionRes> getMyReaction(
            @PathVariable Long cocktailId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = memberService.getMemberByCredentialId(customOAuth2User.getCredentialId()).getId();
        ReactionRes response = cocktailService.getReactionStatus(memberId, cocktailId);
        return ResponseEntity.ok(response);
    }

    @Override
    @Operation(summary = "칵테일 반응 토글", description = "🔒 **인증 필요** - 로그인한 사용자의 칵테일 반응(좋아요/싫어요)을 추가/변경/삭제합니다.")
    @PostMapping("/{cocktailId}/reactions")
    public ResponseEntity<ReactionRes> toggleReaction(
            @PathVariable Long cocktailId,
            @RequestBody ReactionReq request,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = memberService.getMemberByCredentialId(customOAuth2User.getCredentialId()).getId();
        ReactionRes response = cocktailService.toggleReaction(memberId, cocktailId, request.getReactionType());
        return ResponseEntity.ok(response);
    }
}