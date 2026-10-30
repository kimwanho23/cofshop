package kwh.cofshop.global.exception.errorcodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WebRequestErrorCode implements ErrorCode {

    RESOURCE_NOT_FOUND("WEB-404", HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("WEB-405", HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    NOT_ACCEPTABLE("WEB-406", HttpStatus.NOT_ACCEPTABLE, "지원하지 않는 응답 형식입니다."),
    UNSUPPORTED_MEDIA_TYPE("WEB-415", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

}
