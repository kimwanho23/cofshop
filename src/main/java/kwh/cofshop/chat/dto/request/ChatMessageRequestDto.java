package kwh.cofshop.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kwh.cofshop.chat.domain.MessageType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequestDto {

    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long roomId; // 채팅방 번호

    @NotBlank(message = "메시지는 필수입니다.")
    private String message; // 메시지 내용

    private String messageGroupId; // UUID 기반 그룹 ID

    @NotNull(message = "메시지 타입은 필수입니다.")
    private MessageType messageType; // 메시지 타입 구분
}
