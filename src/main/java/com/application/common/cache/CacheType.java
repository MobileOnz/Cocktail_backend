package com.application.common.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CacheType {
    PARSED_MEMBER("parsedMember", 10L, 1000L),
    MAPPING_TASTES("mappingTastes", 30L, 1000L);

    private final String name;
    private final Long expiredAfterWrite;
    private final Long maximumSize;
}
