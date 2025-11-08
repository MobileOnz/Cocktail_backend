package com.application.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ResponseDto<T> {
    private final Integer code; // -1 :실패 , 1: 성공
    private final String msg;
    private final T data; // json data

}
