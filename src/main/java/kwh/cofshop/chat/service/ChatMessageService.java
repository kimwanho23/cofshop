package kwh.cofshop.chat.service;

import kwh.cofshop.chat.domain.ChatMessage;
import kwh.cofshop.chat.domain.ChatRoom;
import kwh.cofshop.chat.dto.request.ChatMessageRequestDto;
import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import kwh.cofshop.chat.dto.response.DeletedMessageResponseDto;
import kwh.cofshop.chat.mapper.ChatMessageMapper;
import kwh.cofshop.chat.repository.ChatMessageRepository;
import kwh.cofshop.chat.repository.ChatRoomRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.ForbiddenErrorCode;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberReadPort memberReadPort;
    private final ChatMessageMapper chatMessageMapper;

    // 채팅 메시지 생성
    @Transactional
    public ChatMessageResponseDto createChatMessage(ChatMessageRequestDto requestDto, Long memberId) {
        // 1. 채팅방 탐색
        ChatRoom chatRoom = chatRoomRepository.findById(requestDto.getRoomId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CHAT_ROOM_NOT_FOUND));

        if (chatRoom.isClosed()) {
            throw new BusinessException(BusinessErrorCode.CHAT_ROOM_ALREADY_CLOSED);
        }

        // 2. 인증된 유저 탐색
        Member sender = memberReadPort.getById(memberId);

        // 3. 해당 유저가 채팅방 참여자인지 검증
        if (!chatRoom.isParticipant(sender)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        // 4. 메시지 생성 후 저장
        ChatMessage message = ChatMessage.createMessage(
                chatRoom,
                sender,
                requestDto.getMessage(),
                requestDto.getMessageGroupId(),
                requestDto.getMessageType()
        );
        chatMessageRepository.save(message);

        return chatMessageMapper.toResponseDto(message);
    }


    // 채팅 메시지 조회
    @Transactional(readOnly = true)
    public Slice<ChatMessageResponseDto> getChatMessages(Long roomId, Long lastMessageId, int pageSize, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CHAT_ROOM_NOT_FOUND));
        Member member = memberReadPort.getById(memberId);

        if (!chatRoom.isParticipant(member)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        return chatMessageRepository.findMessagesByRoom(roomId, lastMessageId, pageSize);
    }

    // 메시지 삭제
    @Transactional
    public DeletedMessageResponseDto deleteMessage(Long roomId, Long messageId, Long senderId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CHAT_MESSAGE_NOT_FOUND));

        if (!message.getChatRoom().getId().equals(roomId)) {
            throw new BusinessException(BusinessErrorCode.CHAT_MESSAGE_NOT_FOUND);
        }

        if (!message.getSender().getId().equals(senderId)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.MEMBER_UNAUTHORIZED);
        }

        String groupId = message.getMessageGroupId();

        // 동일 채팅방 + 동일 작성자의 같은 그룹 메시지만 삭제
        List<ChatMessage> messages =
                chatMessageRepository.findAllByMessageGroupIdAndSender_IdAndChatRoom_Id(groupId, senderId, roomId);
        messages.forEach(ChatMessage::markAsDeleted);

        List<Long> deletedMessageIds = messages.stream()
                .map(ChatMessage::getId)
                .toList();

        return DeletedMessageResponseDto.of(roomId, groupId, deletedMessageIds);
    }

}
