package kwh.cofshop.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import kwh.cofshop.chat.domain.MessageType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequestDto {

    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long roomId; // 채팅방 번호

    @NotNull(message = "전송자 ID는 필수입니다.")
    private Long senderId; // 전송자

    private String message; // 메시지 내용
    private String messageGroupId; // UUID 기반 그룹핑
    private MessageType MessageType; // 메시지 타입 구분
}
