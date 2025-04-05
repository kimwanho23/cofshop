package kwh.cofshop.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import kwh.cofshop.TestSettingUtils;
import kwh.cofshop.chat.dto.response.ChatRoomResponseDto;
import kwh.cofshop.member.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Slf4j
class ChatRoomServiceTest extends TestSettingUtils {

    @Autowired
    private ChatRoomService chatRoomService;

    @Test
    @DisplayName("채팅방 생성")
    @Transactional
    public void createChatTest() throws JsonProcessingException {
        Member member = createMember();

        ChatRoomResponseDto chatRoom = chatRoomService.createChatRoom(member.getId());

        log.info(objectMapper.writeValueAsString(chatRoom));
    }
}