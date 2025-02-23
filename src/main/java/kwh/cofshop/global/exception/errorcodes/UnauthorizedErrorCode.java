package kwh.cofshop.global.exception.errorcodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UnauthorizedErrorCode implements ErrorCode {

    // 401 Unauthorized
    MEMBER_SUSPENDED("U-1", HttpStatus.UNAUTHORIZED, "정지된 회원입니다."),
    MEMBER_QUIT("U-2", HttpStatus.UNAUTHORIZED, "탈퇴한 회원입니다."),
    MEMBER_UNAUTHORIZED("U-3", HttpStatus.UNAUTHORIZED, "권한이 없습니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
