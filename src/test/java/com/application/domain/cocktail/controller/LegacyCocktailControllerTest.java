package com.application.domain.cocktail.controller;

import com.application.common.response.ResponseDto;
import com.application.domain.cocktail.dto.CocktailDto;
import com.application.domain.cocktail.service.CocktailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyCocktailControllerTest {

    @Mock
    private CocktailService cocktailService;

    @InjectMocks
    private LegacyCocktailController controller;

    @Test
    void returnsLegacyRecipeBookShapeAlongsideOriginalFields() {
        CocktailDto dto = new CocktailDto();
        dto.setId(1L);
        dto.setCocktailKR("아메리카노");
        dto.setCocktailEN("Americano");
        dto.setImageUrl("https://cdn.example.com/americano.webp");
        dto.setAbvBand("WEAK");

        when(cocktailService.getCocktailInfo(1L)).thenReturn(dto);

        ResponseEntity<ResponseDto<Map<String, Object>>> response = controller.getCocktail(1L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(1);
        assertThat(response.getBody().getData())
                .containsEntry("id", 1L)
                .containsEntry("cocktailKR", "아메리카노")
                .containsEntry("imageUrl", "https://cdn.example.com/americano.webp");

        @SuppressWarnings("unchecked")
        Map<String, Object> cocktail = (Map<String, Object>) response.getBody().getData().get("cocktail");
        assertThat(cocktail)
                .containsEntry("id", 1L)
                .containsEntry("cocktail_name", "아메리카노")
                .containsEntry("image_url", "https://cdn.example.com/americano.webp");
    }
}
