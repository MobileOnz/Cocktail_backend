package com.application.web.admin;


import com.application.domain.cocktail.dto.CocktailDto;
import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.Ingredient;
import com.application.domain.cocktail.entity.Tag;
import com.application.domain.cocktail.repository.CocktailRepository;
import com.application.domain.cocktail.repository.TagRepository;
import com.application.web.services.cocktail.CocktailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final CocktailService cocktailService;
    private final CocktailRepository cocktailRepository;
    private final TagRepository tagRepository;


    /* 칵테일 관리 */

    // 조회
    public List<CocktailDto> getCocktailFindAll(int page, int size){
        return cocktailService.getCocktailFindAll(page, size);
    }

    //검색
    public List<CocktailDto> getCocktailSearch(int page, int size, String cocktailName){
        return cocktailService.getCocktailSearch(page, size, cocktailName, null, null, null, null);
    }

    public CocktailDto getCocktailInfo(Long cocktailId){
        return cocktailService.getCocktailInfo(cocktailId);
    }

    // 모든 태그 목록 조회
    // 태그타입  + 타입별 값들 selectbox 형태
    public Map<String, Object> getAllTags(){
        return cocktailService.getCocktailTags(null);
    }

    // 태그 저장
    @Transactional
    public boolean addTag(AdminController.ReqTagDto reqTagDto){

        try{
            if(reqTagDto.getId() != null){
                Optional<Tag> optional = tagRepository.findById(reqTagDto.getId());
                if(optional.isPresent()){
                    Tag tag = optional.get();
                    tag.update(reqTagDto.getType(), reqTagDto.getName());
                }else{
                    tagRepository.save(new Tag(reqTagDto.getType(), reqTagDto.getName()));
                }
            }else{
                tagRepository.save(new Tag(reqTagDto.getType(), reqTagDto.getName()));
            }

            return true;
        }catch(Exception e){
            log.error(e.getMessage());
            return false;
        }
    }

    //태그 삭제
    @Transactional
    public boolean deleteTag(Long id){
        Optional<Tag> optional = tagRepository.findById(id);
        if(optional.isPresent()){
            tagRepository.deleteById(id);
            return true;
        }else{
            return false;
        }
    }

    // 1. 칵테일 저장
    @Transactional
    public boolean saveCocktail(AdminController.ReqCocktailDto dto){
        try{
            Cocktail saveCocktail = Cocktail.builder()
                    .cocktailEN(dto.getCocktailEN())
                    .cocktailKR(dto.getCocktailKR())
                    .abvBand(dto.getAbvBand())
                    .maxAlcohol(dto.getMaxAlcohol())
                    .minAlcohol(dto.getMinAlcohol())
                    .tasteLevel(dto.getTasteLevel())
                    .originText(dto.getOriginText())
                    .imageUrl(dto.getImageUrl())
                    .seasons(dto.getSeasons())
                    .ingredients(new ArrayList<>())
                    .tags(new ArrayList<>())
                    .build();

            dto.getIngredients().stream()
                    .map( t -> new Ingredient(t.getName(), t.getAmount()))
                    .forEach(saveCocktail::addIngredient);

            dto.getTags().forEach(t -> {
                Tag tag = tagRepository.findById(t.getTagId())
                        .orElseThrow(() -> new RuntimeException("Tag not found, id=" + t.getTagId()));

                saveCocktail.addTag(tag);
            });

            cocktailRepository.save(saveCocktail);

            return true;
        }catch(Exception e){
            log.error("[saveCocktail] Exception occurred: {}", e.toString());
            log.error("[saveCocktail] Exception class: {}", e.getClass().getName());
            // Hibernate 원인 추출
            if (e.getCause() != null) {
                log.error("[saveCocktail] Cause: {}", e.getCause().getMessage());
            }

            return false;
        }
    }


    // 2. 칵테일 삭제
    public boolean delCocktail(Long cocktailId){
        Optional<Cocktail> optionalCocktail = cocktailRepository.findById(cocktailId);
        if(optionalCocktail.isPresent()){
            cocktailRepository.deleteById(cocktailId);
            return true;
        }else{
            return false;
        }
    }

    // 3. 칵테일 수정
    @Transactional
    public boolean uptCocktail(AdminController.UptCocktailDto dto){
        Optional<Cocktail> optionalCocktail = cocktailRepository.findById(dto.getCocktailId());

        if(optionalCocktail.isPresent()){
            Cocktail cocktail = optionalCocktail.get();

            // 기존 재료 삭제
            cocktail.getIngredients().clear();
            dto.getIngredients().stream()
                            .map(t -> new Ingredient(t.getName(), t.getAmount()))
                            .forEach(cocktail::addIngredient);

            // 기존 태그 삭제
            cocktail.getTags().clear();
            dto.getTags().stream()
                            .map(t -> tagRepository.findById(t.getTagId())
                                    .orElseThrow(() -> new RuntimeException("Tag not found")))
                            .forEach(cocktail::addTag);

            //기존 시즌 삭제
            cocktail.getSeasons().clear();
            cocktail.getSeasons().addAll(dto.getSeasons());

            cocktail.update(dto.getCocktailEN(), dto.getCocktailKR()
            , dto.getAbvBand(), dto.getMaxAlcohol(), dto.getMinAlcohol(), dto.getTasteLevel()
            ,dto.getOriginText(), dto.getImageUrl());

            return true;
        }else{
            return false;
        }
    }


}
