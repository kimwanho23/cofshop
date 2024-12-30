package kwh.cofshop.global.exception.errorcodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BusinessErrorCode implements ErrorCode {
    // 500 에러
    BUSINESS_ERROR_CODE("I-1", HttpStatus.OK, "비즈니스 오류");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

}
