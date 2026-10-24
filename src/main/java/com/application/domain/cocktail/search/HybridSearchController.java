package com.application.domain.cocktail.search;

import com.application.common.response.ResponseDto;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v2/cocktails/search")
public class HybridSearchController {
    private final HybridSearchService service;
    private final Environment environment;
    public HybridSearchController(HybridSearchService service, Environment environment) {
        this.service=service; this.environment=environment;
    }
    @GetMapping("/hybrid")
    public ResponseDto<Map<String,Object>> search(
            @RequestParam String query,
            @RequestParam(required=false) List<String> base,
            @RequestParam(required=false) List<String> excludedBase,
            @RequestParam(required=false) List<String> requiredIngredients,
            @RequestParam(required=false) List<String> excludedIngredients,
            @RequestParam(required=false) Integer abvMin,
            @RequestParam(required=false) Integer abvMax,
            @RequestParam(required=false) String season,
            @RequestParam(defaultValue="10") Integer limit,
            @RequestParam(defaultValue="0") Integer offset,
            @RequestParam(defaultValue="false") Boolean debug) {
        boolean development=environment.acceptsProfiles(Profiles.of("local","dev","test"))
                && !environment.acceptsProfiles(Profiles.of("prod","docker"));
        return ResponseDto.onSuccess(service.search(new HybridSearchRequest(query,base,excludedBase,requiredIngredients,excludedIngredients,abvMin,abvMax,season,limit,offset,debug),development));
    }
}
