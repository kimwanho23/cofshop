package kwh.cofshop.global.exception;

import kwh.cofshop.global.exception.errorcodes.ErrorCode;
import lombok.Getter;

@Getter
public class ForbiddenRequestException extends RuntimeException {
    private final ErrorCode errorCode;

    public ForbiddenRequestException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
