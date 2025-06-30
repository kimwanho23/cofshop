package kwh.cofshop.global.exception.errorcodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DataIntegrityViolationErrorCode implements ErrorCode {

    DATA_INTEGRITY_VIOLATION_ERROR_CODE("DATA_INTEGRITY_VIOLATION", HttpStatus.CONFLICT, "데이터 중복 발생");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
