package com.application.common.exception.custom;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class CustomValidException extends RuntimeException {
    private Map<String ,String> errorMap = new HashMap<>();

    public CustomValidException(String message, Map<String, String> errorMap)
    {
        super(message);
        this.errorMap = errorMap;
    }


}
