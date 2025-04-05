package kwh.cofshop.chat.repository.custom;

import kwh.cofshop.chat.domain.ChatMessage;
import org.springframework.data.domain.Slice;

public interface ChatMessageRepositoryCustom {

    Slice<ChatMessage> findMessagesByRoom(Long roomId, Long lastMessageId, int pageSize);
}
