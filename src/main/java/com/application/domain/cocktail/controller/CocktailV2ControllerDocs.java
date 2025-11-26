package com.application.domain.cocktail.controller;

import com.application.domain.cocktail.dto.request.ReactionReq;
import com.application.domain.cocktail.dto.response.ReactionRes;
import com.application.domain.member.entity.ParsedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface CocktailV2ControllerDocs {

    @Operation(summary = "칵테일 상세 조회", description = "칵테일 ID(PK)를 이용하여 특정 칵테일의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            // 1. 조회 성공
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(example = """
            {
              "code": 1,
              "msg": "cocktail info",
              "data": {
                          "id": 1,
                          "cocktail_kr": "맛있는칵테일",  // DTO에 @JsonProperty("cocktail_kr")이 있다고 가정
                          "cocktail_en": "Delicious Cocktail",
                          "max_alcohol": 30,
                          "min_alcohol": 20,
                          "origin_text": "유래 설명...",
                          "image_url": "https://...",
                          "abv_band": "NORMAL",
                          "taste_level": "BEGINNER",
                          "seasons": ["SPRING"],
            
                          "ingredients": [
                              { "name": "체리", "amount": "1개" },
                              { "name": "얼음", "amount": "가득" }
                          ],
            
                          "tags": [
                              {
                                  "type": "GLASS",
                                  "tags": [
                                      { "id": 4, "name": "칵테일잔" }
                                  ]
                              },
                              {
                                  "type": "MOOD",
                                  "tags": [
                                      { "id": 2, "name": "로맨틱" },
                                      { "id": 5, "name": "조용한" }
                                  ]
                              }
                          ]
                      }
            }
            """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (ID가 없거나 유효하지 않음)",
                    content = @Content(schema = @Schema(example = """
            {
              "code": -1,
              "msg": "칵테일이 존재하지 않습니다.",
              "data": null
            }
            """)))

    })
    ResponseEntity<?> getCocktail(@RequestParam Long cocktailId);

    @Operation(summary = "칵테일 전체 조회 (v1, deprecated)", description = "칵테일 백과")
    @ApiResponses(value = {
            // 1. 조회 성공
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": 1,
                                "msg": "cocktails info",
                                "data": [
                                    {
                                        "id": 1,
                                        "cocktail_kr": "맛있는칵테일",
                                        "cocktail_en": "cocktail",
                                        "max_alcohol": 30,
                                        "min_alcohol": 20,
                                        "base": null,
                                        "origin_text": "맛있는 칵테일 입니다.",
                                        "image_url": "https://onzcocktails3.s3.amazonaws.com/cocktail/16faea27-b259-4d20-9077-de4dd1f257c4_Amaretto_Sour_horizontal.jpg",
                                        "abv_band": "NORMAL",
                                        "taste_level": "BEGINNER",
                                        "seasons": [
                                            "SPRING"
                                        ],
                                        "ingredients": [
                                            {
                                                "name": "체리",
                                                "amount": "1개"
                                            },
                                            {
                                                "name": "얼음",
                                                "amount": "큰거"
                                            }
                                        ],
                                        "tags": [
                                            {
                                                "type": "GLASS",
                                                "tags": [
                                                    {
                                                        "id": 4,
                                                        "name": "cocktail"
                                                    }
                                                ]
                                            },
                                            {
                                                "type": "MOOD",
                                                "tags": [
                                                    {
                                                        "id": 2,
                                                        "name": "romantic"
                                                    }
                                                ]
                                            },
                                            {
                                                "type": "FLAVOR",
                                                "tags": [
                                                    {
                                                        "id": 1,
                                                        "name": "sweet"
                                                    }
                                                ]
                                            },
                                            {
                                                "type": "BASE",
                                                "tags": [
                                                    {
                                                        "id": 3,
                                                        "name": "rum"
                                                    }
                                                ]
                                            }
                                        ]
                                    },
                                    {
                                        "id": 2,
                                        "cocktail_kr": "맛없는칵테일",
                                        "cocktail_en": "nococktail",
                                        "max_alcohol": 20,
                                        "min_alcohol": 10,
                                        "base": null,
                                        "origin_text": "이건 진짜 별로인 칵테일",
                                        "image_url": "https://onzcocktails3.s3.amazonaws.com/cocktail/fbbb5abb-84ef-4df9-b4d5-9dc0ae94078e_Aperol_Spritz_horizontal.jpg",
                                        "abv_band": "NORMAL",
                                        "taste_level": "INTERMEDIATE",
                                        "seasons": [
                                            "SUMMER",
                                            "FALL"
                                        ],
                                        "ingredients": [
                                            {
                                                "name": "오렌지",
                                                "amount": "1개"
                                            },
                                            {
                                                "name": "빨대",
                                                "amount": "1개"
                                            }
                                        ],
                                        "tags": [
                                            {
                                                "type": "GLASS",
                                                "tags": [
                                                    {
                                                        "id": 4,
                                                        "name": "cocktail"
                                                    }
                                                ]
                                            },
                                            {
                                                "type": "FLAVOR",
                                                "tags": [
                                                    {
                                                        "id": 1,
                                                        "name": "sweet"
                                                    }
                                                ]
                                            },
                                            {
                                                "type": "BASE",
                                                "tags": [
                                                    {
                                                        "id": 3,
                                                        "name": "rum"
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            }
            """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (ID가 없거나 유효하지 않음)",
                    content = @Content(schema = @Schema(example = """
                            {
                             	"code": 5000,
                             	"message": "칵테일 전체 조회에 실패하였습니다."
                             }
            """)))

    })
    ResponseEntity<?> getCocktailsUnused(@RequestParam(value = "page", required = false, defaultValue = "0") int page,@RequestParam(value = "size", required = false, defaultValue = "10") int size);

    @Operation(summary = "칵테일 검색", description = "칵테일 백과")
    @ApiResponses(value = {
            // 1. 조회 성공
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": 1,
                                "msg": "serach result",
                                "data": [
                                    {
                                        "id": 3,
                                        "cocktail_kr": "맛있는칵테일",
                                        "cocktail_en": "cocktail",
                                        "max_alcohol": 20,
                                        "min_alcohol": 10,
                                        "origin_text": "맛있는 칵테일!",
                                        "image_url": "https://onzcocktails3.s3.amazonaws.com/cocktail/5ffe4417-d2d4-40d0-bb22-30d1951f0f3d_Amaretto_Sour_horizontal.jpg",
                                        "abv_band": "WEAK",
                                        "taste_level": "NORMAL",
                                        "seasons": [
                                            "SPRING",
                                            "SUMMER"
                                        ],
                                        "ingredients": [
                                            {
                                                "name": "테스트",
                                                "amount": "20p"
                                            },
                                            {
                                                "name": "rum",
                                                "amount": "10ml"
                                            }
                                        ],
                                        "tags": [
                                            {
                                                "type": "GLASS",
                                                "tags": [
                                                    {
                                                        "id": 3,
                                                        "name": "cocktail"
                                                    },
                                                    {
                                                        "id": 6,
                                                        "name": "beer"
                                                    }
                                                ]
                                            },
                                            {
                                                "type": "MOOD",
                                                "tags": [
                                                    {
                                                        "id": 4,
                                                        "name": "romantic"
                                                    }
                                                ]
                                            },
                                            {
                                                "type": "FLAVOR",
                                                "tags": [
                                                    {
                                                        "id": 1,
                                                        "name": "spicy"
                                                    }
                                                ]
                                            },
                                            {
                                                "type": "BASE",
                                                "tags": [
                                                    {
                                                        "id": 2,
                                                        "name": "rum"
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            }
            """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (ID가 없거나 유효하지 않음)",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": -1,
                                "msg": "서버 내부 오류 발생",
                                "data": "JSON parse error: Cannot deserialize value of type `com.application.domain.cocktail.enums.AbvLevel` from String \\"ㅁㅁㅁ\\": not one of the values accepted for Enum class: [WEAK, NORMAL, STRONG]"
                            }
            """)))

    })
    ResponseEntity<?> getCocktailSearch(@RequestBody CocktailV2Controller.CocktailSearchRequest request);

    @Operation(summary = "칵테일 연관검색어", description = "칵테일 백과")
    @ApiResponses(value = {
            // 1. 조회 성공
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": 1,
                                "msg": "related Search result",
                                "data": [
                                    "맛있는칵테일"
                                ]
                            }
            """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (ID가 없거나 유효하지 않음)",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": -1,
                                "msg": "서버 내부 오류 발생",
                                "data": "JSON parse error: Cannot deserialize value of type `com.application.domain.cocktail.enums.AbvLevel` from String \\"ㅁㅁㅁ\\": not one of the values accepted for Enum class: [WEAK, NORMAL, STRONG]"
                            }
            """)))

    })
    ResponseEntity<?> getRelatedCocktail(@RequestParam String searchText);

    @Operation(summary = "칵테일 맞춤 조회", description = "칵테일 맞춤조회")
    @ApiResponses(value = {
            // 1. 조회 성공
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(example = """
                           {
                               "code": 1,
                               "msg": "personalize cocktail result",
                               "data": [
                                   {
                                       "id": 3,
                                       "cocktail_kr": "맛있는칵테일",
                                       "cocktail_en": "cocktail",
                                       "max_alcohol": 20,
                                       "min_alcohol": 10,
                                       "origin_text": "맛있는 칵테일!",
                                       "image_url": "https://onzcocktails3.s3.amazonaws.com/cocktail/5ffe4417-d2d4-40d0-bb22-30d1951f0f3d_Amaretto_Sour_horizontal.jpg",
                                       "abv_band": "WEAK",
                                       "taste_level": "NORMAL",
                                       "seasons": [
                                           "SPRING",
                                           "SUMMER"
                                       ],
                                       "ingredients": [
                                           {
                                               "name": "테스트",
                                               "amount": "20p"
                                           },
                                           {
                                               "name": "rum",
                                               "amount": "10ml"
                                           }
                                       ],
                                       "tags": [
                                           {
                                               "type": "GLASS",
                                               "tags": [
                                                   {
                                                       "id": 3,
                                                       "name": "cocktail"
                                                   },
                                                   {
                                                       "id": 6,
                                                       "name": "beer"
                                                   }
                                               ]
                                           },
                                           {
                                               "type": "MOOD",
                                               "tags": [
                                                   {
                                                       "id": 4,
                                                       "name": "romantic"
                                                   }
                                               ]
                                           },
                                           {
                                               "type": "FLAVOR",
                                               "tags": [
                                                   {
                                                       "id": 1,
                                                       "name": "spicy"
                                                   }
                                               ]
                                           },
                                           {
                                               "type": "BASE",
                                               "tags": [
                                                   {
                                                       "id": 2,
                                                       "name": "rum"
                                                   }
                                               ]
                                           }
                                       ]
                                   }
                               ]
                           }
            """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (ID가 없거나 유효하지 않음)",
                    content = @Content(schema = @Schema(example = """
                            {
                                  "code": -1,
                                  "msg": "잘못된 요청",
                                  "data": "No cocktail found"
                              }
            """)))

    })
    ResponseEntity<?> getPersonalCocktail(@RequestBody CocktailV2Controller.CocktailFilter cocktailFilter);

    @Operation(summary = "칵테일 태그 조회", description = "칵테일 조건")
    @ApiResponses(value = {
            // 1. 조회 성공
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": 1,
                                "msg": "tags",
                                "data": {
                                    "GLASS": [
                                        {
                                            "id": 3,
                                            "name": "cocktail"
                                        },
                                        {
                                            "id": 6,
                                            "name": "beer"
                                        }
                                    ],
                                    "MOOD": [
                                        {
                                            "id": 4,
                                            "name": "romantic"
                                        }
                                    ],
                                    "FLAVOR": [
                                        {
                                            "id": 1,
                                            "name": "spicy"
                                        },
                                        {
                                            "id": 5,
                                            "name": "sweet"
                                        }
                                    ],
                                    "BASE": [
                                        {
                                            "id": 2,
                                            "name": "rum"
                                        }
                                    ]
                                }
                            }
            """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (ID가 없거나 유효하지 않음)",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": -1,
                                "msg": "잘못된 요청",
                                "data": ""
                            }
            """)))

    })
    ResponseEntity<?> getCocktailTags(@RequestBody CocktailV2Controller.RequestTagType requestTagType);

    @Operation(summary = "내 반응 조회", description = "페이지 진입 시, 내가 이 칵테일에 어떤 버튼을 눌렀는지 확인합니다.")
    ResponseEntity<ReactionRes> getMyReaction(@PathVariable Long cocktailId, @AuthenticationPrincipal ParsedMember user);

    @Operation(summary = "반응 토글 (추천/어려워요)", description = "버튼 클릭 시 호출. 이미 눌렀으면 취소, 다른 걸 누르면 스위칭됩니다.")
    ResponseEntity<ReactionRes> toggleReaction(@PathVariable Long cocktailId, @RequestBody ReactionReq request, @AuthenticationPrincipal ParsedMember user);
}
