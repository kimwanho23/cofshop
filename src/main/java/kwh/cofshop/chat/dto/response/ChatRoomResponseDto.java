package kwh.cofshop.chat.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomResponseDto {
    private Long roomId;
    private Long customerId;
    private Long agentId;
    private boolean isActive;
}
