package com.application.common.logging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ThreadLocal에 저장할 요청 컨텍스트 정보
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {

    /**
     * HTTP 메서드 (GET, POST, PUT, DELETE 등)
     */
    private String httpMethod;

    /**
     * 요청 URI (/api/v2/cocktails 등)
     */
    private String uri;

    /**
     * Controller 클래스명
     */
    private String controllerName;

    /**
     * Controller 메서드명
     */
    private String controllerMethod;

    /**
     * 요청 시작 시간 (밀리초)
     */
    private long startTime;
}
