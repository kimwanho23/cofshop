package kwh.cofshop.global.exception.errorcodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ForbiddenErrorCode implements ErrorCode {

    ACCESS_DENIED("FORBIDDEN-ACCESS", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    MEMBER_UNAUTHORIZED("FORBIDDEN-MEMBER", HttpStatus.FORBIDDEN, "권한이 없습니다."),
    CHAT_ROOM_ACCESS_DENIED("FORBIDDEN-CHAT-ROOM", HttpStatus.FORBIDDEN, "채팅방 접근이 거부되었습니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
