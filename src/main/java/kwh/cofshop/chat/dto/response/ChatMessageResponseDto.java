package kwh.cofshop.chat.dto.response;

import kwh.cofshop.chat.domain.MessageType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatMessageResponseDto {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String message;
    private String messageGroupId;
    private MessageType messageType;
    private boolean deleted;
    private LocalDateTime createdAt;
}
