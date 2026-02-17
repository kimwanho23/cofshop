package kwh.cofshop.chat.repository.custom;

import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import org.springframework.data.domain.Slice;

public interface ChatMessageRepositoryCustom {

    Slice<ChatMessageResponseDto> findMessagesByRoom(Long roomId, Long lastMessageId, int pageSize);
}
