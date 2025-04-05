package kwh.cofshop.chat.controller;

import kwh.cofshop.chat.dto.request.ChatMessageRequestDto;
import kwh.cofshop.chat.dto.request.DeleteMessageRequestDto;
import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import kwh.cofshop.chat.dto.response.DeletedMessageResponseDto;
import kwh.cofshop.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate template;

    @MessageMapping("/chat.sendMessage")
    public void handleMessage(@Payload ChatMessageRequestDto requestDto) {
        ChatMessageResponseDto responseDto = chatMessageService.createChatMessage(requestDto);

        template.convertAndSend(
                "/topic/chatroom." + requestDto.getRoomId(),
                responseDto
        );
    }

    @MessageMapping("/chat.deleteMessage")
    public void deleteMessage(@Payload DeleteMessageRequestDto requestDto) {
        DeletedMessageResponseDto deletedMessageResponseDto = chatMessageService.deleteMessage(requestDto.getMessageId(), requestDto.getSenderId());

        template.convertAndSend(
                "/topic/chatroom." + requestDto.getRoomId(),
                deletedMessageResponseDto
        );
    }
}
