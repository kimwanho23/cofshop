package kwh.cofshop.global.exception;

import kwh.cofshop.global.exception.errorcodes.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException { // 발생한 예외를 처리해줄 예외 클래스, 언체크 예외(런타임 예외)

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return String.format("BusinessException(code=%s, message=%s)",
                errorCode.getCode(), errorCode.getMessage());
    }
}
