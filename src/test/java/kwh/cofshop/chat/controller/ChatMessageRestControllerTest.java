package kwh.cofshop.chat.controller;

import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import kwh.cofshop.chat.service.ChatMessageService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import kwh.cofshop.support.TestLoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatMessageRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatMessageService chatMessageService;

    @InjectMocks
    private ChatMessageRestController chatMessageRestController;

    @BeforeEach
    void setUp() {
        mockMvc = StandaloneMockMvcFactory.build(
                chatMessageRestController,
                new TestLoginMemberArgumentResolver()
        );
    }

    @Test
    @DisplayName("채팅 메시지 목록 조회")
    void getMessages() throws Exception {
        Slice<ChatMessageResponseDto> response = new SliceImpl<>(List.of(), PageRequest.of(0, 20), false);
        when(chatMessageService.getChatMessages(anyLong(), any(), anyInt(), anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/chat-messages/1/messages")
                        .param("pageSize", "20"))
                .andExpect(status().isOk());
    }
}
