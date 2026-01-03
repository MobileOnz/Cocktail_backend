package com.application.common;

public class Constant {
    public final static String UPLOAD_DIR = "src/main/java/com/application/uploads/";


    public final static int SUCCESS_CODE = 1;
    public final static int NEED_REFRESH_TOKEN_CODE = -2;
    public final static int ERROR_CODE = -1;

    // JWT 토큰 만료 시간 (밀리초)
    public final static long ACCESS_TOKEN_EXPIRATION = 1 * 60 * 1000L; // 1분
    public final static long REFRESH_TOKEN_EXPIRATION = 5 * 60 * 1000L; // 5분
    // public final static long ACCESS_TOKEN_EXPIRATION = 60 * 60 * 1000L; // 1시간
    // public final static long REFRESH_TOKEN_EXPIRATION = 14 * 24 * 60 * 60 * 1000L; // 14일
    public final static long BLACKLIST_EXPIRATION = 60 * 60 * 1000L; // 1시간
    public final static long BLACKLIST_CLEANUP_INTERVAL = 15 * 60 * 1000L; // 15분


    public final static int LITTLE_ALCHOL_MIN = 0;
    public final static int LITTLE_ALCHOL_MAX = 15;
    public final static int MIDDLE_ALCHOL_MIN = 16;
    public final static int MIDDLE_ALCHOL_MAX = 25;
    public final static int STRONG_ALCHOL_MIN = 26;
    public final static int STRONG_ALCHOL_MAX = 99;
}