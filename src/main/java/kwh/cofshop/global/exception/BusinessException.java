package kwh.cofshop.global.exception;

import kwh.cofshop.global.exception.errorcodes.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BusinessException extends RuntimeException { // 발생한 예외를 처리해줄 예외 클래스, 언체크 예외(런타임 예외)

    private final ErrorCode errorCode;
}