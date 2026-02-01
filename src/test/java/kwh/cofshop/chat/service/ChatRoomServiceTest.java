package kwh.cofshop.chat.service;

import kwh.cofshop.chat.domain.ChatRoom;
import kwh.cofshop.chat.domain.ChatRoomStatus;
import kwh.cofshop.chat.dto.response.ChatRoomResponseDto;
import kwh.cofshop.chat.mapper.ChatRoomMapper;
import kwh.cofshop.chat.repository.ChatRoomRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatRoomMapper chatRoomMapper;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Test
    @DisplayName("채팅방 생성: 회원 없음")
    void createChatRoom_memberNotFound() {
        when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.createChatRoom(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("채팅방 생성: 성공")
    void createChatRoom_success() {
        Member customer = createMember(1L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(customer);
        ChatRoomResponseDto responseDto = new ChatRoomResponseDto();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(chatRoomRepository.save(org.mockito.ArgumentMatchers.any(ChatRoom.class))).thenReturn(chatRoom);
        when(chatRoomMapper.toResponseDto(chatRoom)).thenReturn(responseDto);

        ChatRoomResponseDto result = chatRoomService.createChatRoom(1L);

        assertThat(result).isSameAs(responseDto);
    }

    @Test
    @DisplayName("상담원 참여: 채팅방 없음")
    void joinChatAgent_roomNotFound() {
        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.joinChatAgent(1L, 2L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("상담원 참여: 이미 참여됨")
    void joinChatAgent_alreadyHasAgent() {
        Member customer = createMember(1L);
        Member agent = createMember(2L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(customer);
        chatRoom.assignAgent(agent);

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatRoomService.joinChatAgent(1L, 3L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("상담원 참여: 진행중 상태")
    void joinChatAgent_inProgress() {
        Member customer = createMember(1L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(customer);
        ReflectionTestUtils.setField(chatRoom, "chatRoomStatus", ChatRoomStatus.IN_PROGRESS);

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatRoomService.joinChatAgent(1L, 2L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("상담원 참여: 상담원 없음")
    void joinChatAgent_agentNotFound() {
        Member customer = createMember(1L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(customer);

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.joinChatAgent(1L, 2L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("상담원 참여: 성공")
    void joinChatAgent_success() {
        Member customer = createMember(1L);
        Member agent = createMember(2L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(customer);

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(agent));

        chatRoomService.joinChatAgent(1L, 2L);

        assertThat(chatRoom.getChatRoomStatus()).isEqualTo(ChatRoomStatus.IN_PROGRESS);
        assertThat(chatRoom.getAgent()).isEqualTo(agent);
    }

    @Test
    @DisplayName("채팅방 종료: 채팅방 없음")
    void closeChatRoom_notFound() {
        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.closeChatRoom(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("채팅방 종료: 이미 종료")
    void closeChatRoom_alreadyClosed() {
        Member customer = createMember(1L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(customer);
        chatRoom.close();

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatRoomService.closeChatRoom(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("채팅방 종료: 성공")
    void closeChatRoom_success() {
        Member customer = createMember(1L);
        ChatRoom chatRoom = ChatRoom.createChatRoom(customer);

        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

        chatRoomService.closeChatRoom(1L);

        assertThat(chatRoom.getChatRoomStatus()).isEqualTo(ChatRoomStatus.CLOSED);
        verify(chatRoomRepository).findById(1L);
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