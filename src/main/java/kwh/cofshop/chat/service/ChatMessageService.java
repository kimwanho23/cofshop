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
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageMapper chatMessageMapper;

    // 채팅 메시지 생성
    @Transactional
    public ChatMessageResponseDto createChatMessage(ChatMessageRequestDto requestDto) {
        // 1. 채팅방 탐색
        ChatRoom chatRoom = chatRoomRepository.findById(requestDto.getRoomId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CHATROOM_NOT_FOUND));

        // 2. 유저 탐색
        Member sender = memberRepository.findById(requestDto.getSenderId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        //3. 메시지 생성 후 저장
        ChatMessage message = ChatMessage.createMessage(chatRoom, sender, requestDto);
        chatMessageRepository.save(message);

        return chatMessageMapper.toResponseDto(message);
    }

    // 채팅 메시지 조회
    @Transactional(readOnly = true)
    public Slice<ChatMessageResponseDto> getChatMessages(Long roomId, Long lastMessageId, int pageSize) {
        // 1. Slice로 메시지 조각을 가져온다.
        Slice<ChatMessage> slice = chatMessageRepository.findMessagesByRoom(roomId, lastMessageId, pageSize);

        // 2. DTO로 매핑
        List<ChatMessageResponseDto> dtoList = slice.getContent().stream()
                .map(chatMessageMapper::toResponseDto)
                .toList();

        return new SliceImpl<>(dtoList, slice.getPageable(), slice.hasNext());
    }

    // 메시지 삭제
    @Transactional
    public DeletedMessageResponseDto deleteMessage(Long messageId, Long senderId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BUSINESS_ERROR_CODE));

        if (!message.getSender().getId().equals(senderId)) {
            throw new BusinessException(UnauthorizedErrorCode.MEMBER_UNAUTHORIZED);
        }

        String groupId = message.getMessageGroupId();

        // 그룹에 속한 전체 메시지 삭제
        List<ChatMessage> messages = chatMessageRepository.findAllByMessageGroupId(groupId);
        messages.forEach(ChatMessage::markAsDeleted);

        List<Long> deletedMessageIds = messages.stream()
                .map(ChatMessage::getId)
                .toList();

        DeletedMessageResponseDto responseDto = new DeletedMessageResponseDto();
        responseDto.setGroupId(groupId);
        responseDto.setMessageIds(deletedMessageIds);

        return responseDto;
    }

}
