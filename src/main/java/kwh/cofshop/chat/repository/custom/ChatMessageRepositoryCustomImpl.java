package kwh.cofshop.chat.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.chat.domain.QChatMessage;
import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
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
    public Slice<ChatMessageResponseDto> findMessagesByRoom(Long roomId, Long lastMessageId, int pageSize) {
        QChatMessage chatMessage = QChatMessage.chatMessage;

        List<ChatMessageResponseDto> content = queryFactory
                .select(Projections.fields(
                        ChatMessageResponseDto.class,
                        chatMessage.id.as("messageId"),
                        chatMessage.chatRoom.id.as("roomId"),
                        chatMessage.sender.id.as("senderId"),
                        chatMessage.message,
                        chatMessage.messageGroupId,
                        chatMessage.messageType,
                        chatMessage.isDeleted.as("deleted"),
                        chatMessage.createDate.as("createdAt")
                ))
                .from(chatMessage)
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
