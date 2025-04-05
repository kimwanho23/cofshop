package kwh.cofshop.chat.dto.request;

import kwh.cofshop.chat.domain.MessageType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequestDto {
    private Long roomId; // 채팅방 번호
    private Long senderId; // 전송자
    private String message; // 메시지 내용
    private String messageGroupId; // UUID 기반 그룹핑
    private MessageType MessageType; // 메시지 타입 구분
}
