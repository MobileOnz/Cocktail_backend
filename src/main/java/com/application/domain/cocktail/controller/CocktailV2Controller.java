package com.application.domain.cocktail.controller;

import com.application.common.response.ResponseDto;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.enums.Season;
import com.application.domain.cocktail.enums.TasteLevel;

import com.application.domain.cocktail.service.CocktailService;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/cocktails")
@RequiredArgsConstructor
public class CocktailV2Controller {
    private final CocktailService cocktailService;

    @GetMapping("/{cocktailId}")
    public ResponseEntity<?> getCocktail(@RequestParam Long cocktailId){
        return new ResponseEntity<>(new ResponseDto<>(1, "cocktail info", cocktailService.getCocktailInfo(cocktailId)),HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<?> getCocktails(@RequestParam(value = "page", required = false, defaultValue = "0") int page,@RequestParam(value = "size", required = false, defaultValue = "10") int size){
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
    @PostMapping("/cocktail/tags")
    public ResponseEntity<?> getCocktailTags(@RequestBody RequestTagType requestTagType){
        return new ResponseEntity<>(new ResponseDto<>(1, "tags", cocktailService.getCocktailTags(requestTagType)), HttpStatus.OK);
    }

}
