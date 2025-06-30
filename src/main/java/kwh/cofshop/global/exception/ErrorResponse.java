package kwh.cofshop.global.exception;

import kwh.cofshop.global.exception.errorcodes.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class ErrorResponse {

    private final String codes;         // 비즈니스 에러 코드 (예: F-1)
    private final HttpStatus status;          // HTTP 상태 코드
    private final String message;      // 에러 메시지
    private final LocalDateTime timestamp;  // 에러 발생 시각

    // 생성자
    private ErrorResponse(final ErrorCode errorCode) {
        this.codes = errorCode.getCode();
        this.status = errorCode.getHttpStatus();
        this.message = errorCode.getMessage();
        this.timestamp = LocalDateTime.now();
    }

    private ErrorResponse(String codes, HttpStatus status, String message, LocalDateTime timestamp) {
        this.codes = codes;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }


    // 기본
    public static ErrorResponse of(final ErrorCode errorCode) {
        return new ErrorResponse(errorCode);
    }

    public static ErrorResponse of(final ErrorCode errorCode, final String customMessage) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getHttpStatus(),
                customMessage,
                LocalDateTime.now()
        );
    }

    public static ErrorResponse of(final HttpStatus status, final String message) {
        return new ErrorResponse(
                "COMMON",
                status,
                message,
                LocalDateTime.now()
        );
    }
}
