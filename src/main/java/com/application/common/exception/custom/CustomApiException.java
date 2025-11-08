package com.application.common.exception.custom;

import lombok.Getter;

@Getter
public class CustomApiException extends RuntimeException{
    public CustomApiException(String message){
        super(message);
    }
}
