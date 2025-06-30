package kwh.cofshop.chat.controller;

import kwh.cofshop.chat.dto.request.ChatMessageRequestDto;
import kwh.cofshop.chat.dto.request.DeleteMessageRequestDto;
import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import kwh.cofshop.chat.dto.response.DeletedMessageResponseDto;
import kwh.cofshop.chat.service.ChatMessageService;
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@RequiredArgsConstructor
@Controller
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate template;

    @MessageMapping("/chat.sendMessage")
    public void handleMessage(@Payload ChatMessageRequestDto requestDto, Principal principal) { // Principal을 사용해서 인증 사용자 정보를 가져온다
        CustomUserDetails userDetails = (CustomUserDetails) ((Authentication) principal).getPrincipal();
        Long memberId = userDetails.getId();
        ChatMessageResponseDto responseDto = chatMessageService.createChatMessage(requestDto, memberId);

        template.convertAndSend(
                "/topic/chatroom." + requestDto.getRoomId(),
                responseDto
        );
    }

    @MessageMapping("/chat.deleteMessage")
    public void deleteMessage(@Payload DeleteMessageRequestDto requestDto, Principal principal) {
        CustomUserDetails userDetails = (CustomUserDetails) ((Authentication) principal).getPrincipal();
        Long senderId = userDetails.getId();
        DeletedMessageResponseDto responseDto = chatMessageService.deleteMessage(requestDto.getMessageId(), senderId);

        template.convertAndSend(
                "/topic/chatroom." + requestDto.getRoomId(),
                responseDto
        );
    }

}
