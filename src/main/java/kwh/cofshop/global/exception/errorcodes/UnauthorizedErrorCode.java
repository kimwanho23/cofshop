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
    MEMBER_UNAUTHORIZED("U-3", HttpStatus.UNAUTHORIZED, "권한이 없습니다."),
    CHAT_ROOM_ACCESS_DENIED("U-4", HttpStatus.UNAUTHORIZED, "채팅방 접근이 거부되었습니다."),

    TOKEN_EXPIRED("U-EXPIRED", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID("U-INVALID", HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다."),
    TOKEN_NOT_EXIST("U-NOT_EXIST", HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다.")
    ;

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
