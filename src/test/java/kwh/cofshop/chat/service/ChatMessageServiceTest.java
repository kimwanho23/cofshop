package kwh.cofshop.chat.service;

import kwh.cofshop.chat.domain.ChatMessage;
import kwh.cofshop.chat.domain.ChatRoom;
import kwh.cofshop.chat.domain.MessageType;
import kwh.cofshop.chat.dto.request.ChatMessageRequestDto;
import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import kwh.cofshop.chat.dto.response.DeletedMessageResponseDto;
import kwh.cofshop.chat.mapper.ChatMessageMapper;
import kwh.cofshop.chat.repository.ChatMessageRepository;
import kwh.cofshop.chat.repository.ChatRoomRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MemberReadPort memberReadPort;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("메시지 생성: 채팅방 없음")
    void createChatMessage_roomNotFound() {
        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.empty());

        ChatMessageRequestDto requestDto = new ChatMessageRequestDto();
        requestDto.setRoomId(1L);
        requestDto.setMessageType(MessageType.TEXT);

        assertThatThrownBy(() -> chatMessageService.createChatMessage(requestDto, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("메시지 생성: 회원 없음")
    void createChatMessage_memberNotFound() {
        ChatRoom chatRoom = ChatRoom.createChatRoom(createMember(1L));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(memberReadPort.getById(2L)).thenThrow(new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        ChatMessageRequestDto requestDto = new ChatMessageRequestDto();
        requestDto.setRoomId(1L);
        requestDto.setMessageType(MessageType.TEXT);

        assertThatThrownBy(() -> chatMessageService.createChatMessage(requestDto, 2L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("메시지 생성: 참여자 아님")
    void createChatMessage_notParticipant() {
        ChatRoom chatRoom = ChatRoom.createChatRoom(createMember(1L));
        Member sender = createMember(2L);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(memberReadPort.getById(2L)).thenReturn(sender);

        ChatMessageRequestDto requestDto = new ChatMessageRequestDto();
        requestDto.setRoomId(1L);
        requestDto.setMessageType(MessageType.TEXT);

        assertThatThrownBy(() -> chatMessageService.createChatMessage(requestDto, 2L))
                .isInstanceOf(ForbiddenRequestException.class);
    }

    @Test
    @DisplayName("메시지 생성: 성공")
    void createChatMessage_success() {
        Member member = createMember(1L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(member);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(memberReadPort.getById(1L)).thenReturn(member);
        when(chatMessageMapper.toResponseDto(any(ChatMessage.class))).thenReturn(new ChatMessageResponseDto());

        ChatMessageRequestDto requestDto = new ChatMessageRequestDto();
        requestDto.setRoomId(1L);
        requestDto.setMessage("안녕하세요");
        requestDto.setMessageType(MessageType.TEXT);

        ChatMessageResponseDto result = chatMessageService.createChatMessage(requestDto, 1L);

        assertThat(result).isNotNull();
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("메시지 생성: 같은 ID 다른 인스턴스도 참여자로 인정")
    void createChatMessage_success_sameIdDifferentInstance() {
        Member customer = createMember(1L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(customer);
        Member loadedMember = createMember(1L);

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(memberReadPort.getById(1L)).thenReturn(loadedMember);
        when(chatMessageMapper.toResponseDto(any(ChatMessage.class))).thenReturn(new ChatMessageResponseDto());

        ChatMessageRequestDto requestDto = new ChatMessageRequestDto();
        requestDto.setRoomId(1L);
        requestDto.setMessage("같은 회원");
        requestDto.setMessageType(MessageType.TEXT);

        ChatMessageResponseDto result = chatMessageService.createChatMessage(requestDto, 1L);

        assertThat(result).isNotNull();
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("메시지 생성: 종료된 채팅방")
    void createChatMessage_closedRoom() {
        Member customer = createMember(1L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(customer);
        chatRoom.close();

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

        ChatMessageRequestDto requestDto = new ChatMessageRequestDto();
        requestDto.setRoomId(1L);
        requestDto.setMessageType(MessageType.TEXT);

        assertThatThrownBy(() -> chatMessageService.createChatMessage(requestDto, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("채팅 메시지 조회")
    void getChatMessages() {
        Member member = createMember(1L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(member);

        ChatMessage message = ChatMessage.builder()
                .message("안녕하세요")
                .messageGroupId("group")
                .build();
        ReflectionTestUtils.setField(message, "id", 10L);

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(memberReadPort.getById(1L)).thenReturn(member);
        Slice<ChatMessage> slice = new SliceImpl<>(List.of(message), PageRequest.of(0, 20), false);
        when(chatMessageRepository.findMessagesByRoom(anyLong(), any(), anyInt())).thenReturn(slice);
        when(chatMessageMapper.toResponseDto(message)).thenReturn(new ChatMessageResponseDto());

        Slice<ChatMessageResponseDto> result = chatMessageService.getChatMessages(1L, null, 20, 1L);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("채팅 메시지 조회: 참여자 아님")
    void getChatMessages_notParticipant() {
        Member customer = createMember(1L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(customer);
        Member outsider = createMember(2L);

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(memberReadPort.getById(2L)).thenReturn(outsider);

        assertThatThrownBy(() -> chatMessageService.getChatMessages(1L, null, 20, 2L))
                .isInstanceOf(ForbiddenRequestException.class);
    }

    @Test
    @DisplayName("메시지 삭제: 메시지 없음")
    void deleteMessage_notFound() {
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatMessageService.deleteMessage(1L, 1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("메시지 삭제: 권한 없음")
    void deleteMessage_notAuthorized() {
        Member sender = createMember(1L);
        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .chatRoom(ChatRoom.builder().id(1L).build())
                .messageGroupId("group")
                .build();
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> chatMessageService.deleteMessage(1L, 1L, 2L))
                .isInstanceOf(ForbiddenRequestException.class);
    }

    @Test
    @DisplayName("메시지 삭제: 다른 채팅방 메시지")
    void deleteMessage_differentRoom() {
        Member sender = createMember(1L);
        ChatRoom chatRoom = ChatRoom.builder().id(2L).build();
        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .chatRoom(chatRoom)
                .messageGroupId("group")
                .build();
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> chatMessageService.deleteMessage(1L, 1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("메시지 삭제: 같은 그룹이라도 다른 작성자 메시지는 제외")
    void deleteMessage_onlySenderMessages() {
        Member sender = createMember(1L);
        ChatRoom chatRoom = ChatRoom.builder().id(1L).build();
        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .chatRoom(chatRoom)
                .messageGroupId("group")
                .build();
        ReflectionTestUtils.setField(message, "id", 10L);
        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));
        when(chatMessageRepository.findAllByMessageGroupIdAndSender_IdAndChatRoom_Id("group", 1L, 1L))
                .thenReturn(List.of(message));

        DeletedMessageResponseDto result = chatMessageService.deleteMessage(1L, 10L, 1L);

        assertThat(result.getMessageIds()).containsExactly(10L);
        assertThat(message.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("메시지 삭제: 성공")
    void deleteMessage_success() {
        Member sender = createMember(1L);
        ChatRoom chatRoom = ChatRoom.builder().id(1L).build();
        ChatMessage message1 = ChatMessage.builder()
                .sender(sender)
                .chatRoom(chatRoom)
                .messageGroupId("group")
                .build();
        ChatMessage message2 = ChatMessage.builder()
                .sender(sender)
                .chatRoom(chatRoom)
                .messageGroupId("group")
                .build();
        ReflectionTestUtils.setField(message1, "id", 1L);
        ReflectionTestUtils.setField(message2, "id", 2L);

        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(message1));
        when(chatMessageRepository.findAllByMessageGroupIdAndSender_IdAndChatRoom_Id("group", 1L, 1L))
                .thenReturn(List.of(message1, message2));

        DeletedMessageResponseDto result = chatMessageService.deleteMessage(1L, 1L, 1L);

        assertThat(result.getGroupId()).isEqualTo("group");
        assertThat(result.getMessageIds()).containsExactly(1L, 2L);
        assertThat(message1.isDeleted()).isTrue();
        assertThat(message2.isDeleted()).isTrue();
    }

    private Member createMember(Long id) {
        return Member.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .memberName("사용자" + id)
                .memberPwd("pw")
                .tel("01012341234")
                .build();
    }
}
