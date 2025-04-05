package kwh.cofshop.chat.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.chat.domain.ChatMessage;
import kwh.cofshop.chat.domain.QChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryCustomImpl implements ChatMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<ChatMessage> findMessagesByRoom(Long roomId, Long lastMessageId, int pageSize) {
        QChatMessage chatMessage = QChatMessage.chatMessage;

        List<ChatMessage> content = queryFactory
                .selectFrom(chatMessage)
                .where(
                        chatMessage.chatRoom.id.eq(roomId),
                        lastMessageId != null ?
                                chatMessage.id.lt(lastMessageId) : null
                )
                .orderBy(chatMessage.id.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = content.size() > pageSize;

        if (hasNext) {
            content.remove(pageSize);
        }

        return new SliceImpl<>(content, PageRequest.of(0, pageSize), hasNext);
    }
}
