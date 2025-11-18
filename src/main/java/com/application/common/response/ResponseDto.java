package com.application.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "공통 응답 객체")
public class ResponseDto<T> {

    @Schema(description = "응답 코드 (1: 성공, -1: 실패, -2: 토큰만료 등)", example = "1")
    private final Integer code; // -1 :실패 , 1: 성공

    @Schema(description = "응답 메시지", example = "요청이 성공하였습니다.")
    private final String msg;

    @Schema(description = "응답 데이터 (결과가 없으면 null)")
    private final T data; // json data

    public static <T> ResponseDto<T> onSuccess(T data) {
        return new ResponseDto<>(1, "성공", data);
    }

    public static <T> ResponseDto<T> onSuccess() {
        return new ResponseDto<>(1, "성공", null);
    }

    public static <T> ResponseDto<T> onSuccess(String msg, T data) {
        return new ResponseDto<>(1, msg, data);
    }

    public static <T> ResponseDto<T> onFail(int errorCode, String message) {
        return new ResponseDto<>(errorCode, message, null);
    }
}
