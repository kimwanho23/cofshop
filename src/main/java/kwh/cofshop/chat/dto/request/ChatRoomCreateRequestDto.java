package kwh.cofshop.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomCreateRequestDto { // 채팅방 생성 Request
    @NotNull(message = "고객 ID는 필수입니다.")
    private Long customerId;
}
