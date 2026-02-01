package kwh.cofshop.chat.controller;

import kwh.cofshop.chat.dto.response.ChatRoomResponseDto;
import kwh.cofshop.chat.service.ChatRoomService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import kwh.cofshop.support.TestLoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatRoomControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatRoomController chatRoomController;

    @BeforeEach
    void setUp() {
        mockMvc = StandaloneMockMvcFactory.build(
                chatRoomController,
                new TestLoginMemberArgumentResolver()
        );
    }

    @Test
    @DisplayName("채팅방 생성")
    void createChatRoom() throws Exception {
        ChatRoomResponseDto responseDto = new ChatRoomResponseDto();
        responseDto.setRoomId(1L);

        when(chatRoomService.createChatRoom(anyLong())).thenReturn(responseDto);

        mockMvc.perform(post("/api/chat-rooms"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("상담사 배정")
    void joinChatAgent() throws Exception {
        mockMvc.perform(patch("/api/chat-rooms/1/join")
                        .param("agentId", "2"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("채팅방 종료")
    void closeChatRoom() throws Exception {
        mockMvc.perform(patch("/api/chat-rooms/1/close"))
                .andExpect(status().isNoContent());
    }
}
