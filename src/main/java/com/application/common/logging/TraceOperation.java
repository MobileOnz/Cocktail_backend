package com.application.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 레이어 어노테이션이 없는 Spring Bean도 필요한 지점만 AOP 추적하기 위한 명시적 표시다. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceOperation {}
