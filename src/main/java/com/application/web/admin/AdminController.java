package com.application.web.admin;


import com.application.common.response.ResponseDto;
import com.application.common.s3.S3Service;
import com.application.domain.cocktail.dto.CocktailDto;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.enums.Season;
import com.application.domain.cocktail.enums.TagType;
import com.application.domain.cocktail.enums.TasteLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

//@Controller
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final S3Service s3Service;

    public AdminController(AdminService adminService, S3Service s3Service) {
        this.adminService = adminService;
        this.s3Service = s3Service;
    }

    @GetMapping("/login")
    public String login(Model model, String error, String logout) {
        if(error != null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        if(logout != null) {
            model.addAttribute("message", "정상적으로 로그아웃되었습니다.");
        }
        return "admin/login";
    }


    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("title", "관리자 대시보드");
        return "admin/dashboard";
    }



    @GetMapping("/cocktails")
    public String cocktails(@RequestParam(required = false, defaultValue = "0") Integer page, @RequestParam(required = false, defaultValue = "30") Integer size, Model model) {
        List<CocktailDto> cocktails = adminService.getCocktailFindAll(page, size);
        model.addAttribute("cocktails", cocktails);
        return "admin/cocktails";
    }

    @GetMapping("/search/cocktail")
    @ResponseBody
    public ResponseEntity<?> searchCocktail(@RequestParam(required = false, defaultValue = "0") Integer page,
                                            @RequestParam(required = false, defaultValue = "30") Integer size,
                                            @RequestParam(required = false, defaultValue = "") String cocktailName){
        List<CocktailDto> cocktailSearch = adminService.getCocktailSearch(page, size, cocktailName);
        return new ResponseEntity<>(new ResponseDto<>(1, "success", cocktailSearch), HttpStatus.OK);
    }


    //TODO) cocktail 상세정보 수정필요 > 등록/수정시에 따른 페이지 처리 필요 (admin/cocktail/detail)
    @GetMapping({"/cocktails/detail", "/cocktail/detail"})
    public String cocktail(){
        return "admin/cocktail/detail";
    }

    //TODO) 칵테일 정보 조회
    @GetMapping("/cocktail/info")
    @ResponseBody
    public ResponseEntity<?> cocktailInfo(@RequestParam Long cocktailId){
        CocktailDto cocktail = adminService.getCocktailInfo(cocktailId);
        return new ResponseEntity<>(new ResponseDto<>(1, "success", cocktail), HttpStatus.OK);
    }


    //TODO) 태그 종류 및 조회
    @GetMapping("/cocktail/tags")
    public ResponseEntity<?> cocktailTags(){
        Map<String, Object> allTags = adminService.getAllTags();
        return new ResponseEntity<>(new ResponseDto<>(1, "success", allTags), HttpStatus.OK);
    }


    @GetMapping("/cocktail/tag")
    public String templateTag(){
        return "admin/tag";
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReqTagDto{
        private Long id;
        private TagType type;
        private String name;
    }

    @PostMapping("/manage/tag")
    @ResponseBody
    public ResponseEntity<?> manageTag(@RequestBody ReqTagDto dto){
        if(adminService.addTag(dto)){
            return new ResponseEntity<>(new ResponseDto<>(1, "success", dto), HttpStatus.OK);
        }else{
            return new ResponseEntity<>(new ResponseDto<>(-1, "fail", dto), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/manage/tag")
    @ResponseBody
    public ResponseEntity<?> manageTagDelete(@RequestParam Long id){
        if(adminService.deleteTag(id)){
            return new ResponseEntity<>(new ResponseDto<>(1, "success", null), HttpStatus.OK);
        }else{
            return new ResponseEntity<>(new ResponseDto<>(-1, "fail", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReqCocktailDto{
        @JsonProperty("cocktailKR")
        private String cocktailKR;

        @JsonProperty("cocktailEN")
        private String cocktailEN;

        @JsonProperty("maxAlcohol")
        private Integer maxAlcohol;

        @JsonProperty("minAlcohol")
        private Integer minAlcohol;

        @JsonProperty("originText")
        private String originText;

        @JsonProperty("imageUrl")
        private String imageUrl;

        @JsonProperty("abvBand")
        private AbvLevel abvBand;

        @JsonProperty("tasteLevel")
        private TasteLevel tasteLevel;

        @JsonProperty("seasons")
        private List<Season> seasons;

        @JsonProperty("ingredients")
        private List<IngredientDto> ingredients;

        @JsonProperty("tags")
        private List<CocktailTagDto> tags;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientDto{
        private String name;
        private String amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CocktailTagDto {
        @JsonProperty("tagId")
        private Long tagId;
    }

    @PostMapping("/cocktails/save")
    @ResponseBody
    public ResponseEntity<?> cocktailSave(@RequestBody ReqCocktailDto dto){
        if(adminService.saveCocktail(dto)){
            return new ResponseEntity<>(new ResponseDto<>(1, "success", null), HttpStatus.OK);
        }else{
            return new ResponseEntity<>(new ResponseDto<>(-1, "fail", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/cocktails/delete")
    @ResponseBody
    public ResponseEntity<?> cocktailDelete(@RequestParam Long cocktailId){
        if(adminService.delCocktail(cocktailId)){
            return new ResponseEntity<>(new ResponseDto<>(1, "success", null), HttpStatus.OK);
        }else{
            return new ResponseEntity<>(new ResponseDto<>(-1, "fail", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UptCocktailDto{
        @JsonProperty("cocktailId")
        private Long cocktailId;

        @JsonProperty("cocktailKR")
        private String cocktailKR;

        @JsonProperty("cocktailEN")
        private String cocktailEN;

        @JsonProperty("maxAlcohol")
        private Integer maxAlcohol;

        @JsonProperty("minAlcohol")
        private Integer minAlcohol;

        @JsonProperty("originText")
        private String originText;

        @JsonProperty("imageUrl")
        private String imageUrl;

        @JsonProperty("abvBand")
        private AbvLevel abvBand;

        @JsonProperty("tasteLevel")
        private TasteLevel tasteLevel;

        @JsonProperty("seasons")
        private List<Season> seasons;

        @JsonProperty("ingredients")
        private List<IngredientDto> ingredients;

        @JsonProperty("tags")
        private List<CocktailTagDto> tags;
    }

    @PostMapping("/cocktails/update")
    @ResponseBody
    public ResponseEntity<?> cocktailUpdate(@RequestBody UptCocktailDto dto){
        if(adminService.uptCocktail(dto)){
            return new ResponseEntity<>(new ResponseDto<>(1, "success", null), HttpStatus.OK);
        }else{
            return new ResponseEntity<>(new ResponseDto<>(-1, "fail", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // 이미지 업로드
    @PostMapping("/cocktail/image/upload")
    @ResponseBody
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file){
        try{
            String imageUrl = s3Service.upload(file, "cocktail");
            return new ResponseEntity<>(new ResponseDto<>(1, "success", imageUrl), HttpStatus.OK);
        }catch(Exception e){
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //기존 이미지 삭제
    @PostMapping("/cocktail/image/delete")
    @ResponseBody
    public ResponseEntity<?> deleteImage(@RequestBody Map<String, Object> param){
        if(param != null && param.get("url") != null) {
            s3Service.delete(param.get("url").toString());
            return new ResponseEntity<>(new ResponseDto<>(1, "success", null), HttpStatus.OK);
        }else{
            return new ResponseEntity<>(new ResponseDto<>(1, "parsing error", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
