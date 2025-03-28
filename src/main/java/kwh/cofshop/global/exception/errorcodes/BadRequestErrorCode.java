package kwh.cofshop.global.exception.errorcodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BadRequestErrorCode implements ErrorCode {


    // 400 Bad Request
    BAD_REQUEST("F-1", HttpStatus.BAD_REQUEST, "잘못된 접근입니다."),
    INPUT_INVALID_VALUE("D", HttpStatus.BAD_REQUEST, "잘못된 입력입니다");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
