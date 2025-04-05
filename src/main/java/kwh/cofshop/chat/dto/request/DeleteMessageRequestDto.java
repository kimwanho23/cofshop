package kwh.cofshop.chat.dto.request;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteMessageRequestDto {
    private Long roomId;      // 채팅방 ID
    private Long messageId;   // 삭제 메시지 ID
    private Long senderId;    // 본인 확인용
}
