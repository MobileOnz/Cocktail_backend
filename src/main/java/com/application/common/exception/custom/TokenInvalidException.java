package com.application.common.exception.custom;

import lombok.Getter;

@Getter
public class TokenInvalidException extends RuntimeException{
    public TokenInvalidException(String message){
        super(message);
    }
}
