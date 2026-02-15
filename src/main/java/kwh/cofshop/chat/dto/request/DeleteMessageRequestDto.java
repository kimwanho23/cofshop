package kwh.cofshop.chat.dto.request;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteMessageRequestDto {
    @NotNull(message = "채팅방 ID는 필수입니다.")
    @Positive(message = "채팅방 ID는 1 이상이어야 합니다.")
    private Long roomId;      // 채팅방 ID

    @NotNull(message = "메시지 ID는 필수입니다.")
    @Positive(message = "메시지 ID는 1 이상이어야 합니다.")
    private Long messageId;   // 삭제 메시지 ID
}
