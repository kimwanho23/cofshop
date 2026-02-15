package kwh.cofshop.chat.controller;

import kwh.cofshop.chat.domain.MessageType;
import kwh.cofshop.chat.dto.request.ChatMessageRequestDto;
import kwh.cofshop.chat.dto.request.DeleteMessageRequestDto;
import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import kwh.cofshop.chat.dto.response.DeletedMessageResponseDto;
import kwh.cofshop.chat.service.ChatMessageService;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.security.userdetails.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTest {

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private SimpMessagingTemplate template;

    @InjectMocks
    private ChatMessageController chatMessageController;

    @Test
    @DisplayName("send message")
    void handleMessage() {
        ChatMessageRequestDto requestDto = new ChatMessageRequestDto();
        requestDto.setRoomId(1L);
        requestDto.setMessage("hello");
        requestDto.setMessageType(MessageType.TEXT);

        ChatMessageResponseDto responseDto = new ChatMessageResponseDto();
        when(chatMessageService.createChatMessage(any(), anyLong())).thenReturn(responseDto);

        Principal principal = buildPrincipal(1L);

        chatMessageController.handleMessage(requestDto, principal);

        verify(template).convertAndSend("/topic/chatroom.1", responseDto);
    }

    @Test
    @DisplayName("delete message")
    void deleteMessage() {
        DeleteMessageRequestDto requestDto = new DeleteMessageRequestDto();
        requestDto.setRoomId(2L);
        requestDto.setMessageId(200L);

        DeletedMessageResponseDto responseDto = new DeletedMessageResponseDto();
        responseDto.setRoomId(2L);

        when(chatMessageService.deleteMessage(anyLong(), anyLong(), anyLong())).thenReturn(responseDto);

        Principal principal = buildPrincipal(1L);

        chatMessageController.deleteMessage(requestDto, principal);

        verify(template).convertAndSend("/topic/chatroom.2", responseDto);
    }

    private Principal buildPrincipal(Long memberId) {
        CustomUserDetails userDetails = new CustomUserDetails(
                memberId,
                "user@example.com",
                "",
                List.of(),
                MemberState.ACTIVE,
                LocalDateTime.now()
        );
        return new TestingAuthenticationToken(userDetails, null);
    }
}