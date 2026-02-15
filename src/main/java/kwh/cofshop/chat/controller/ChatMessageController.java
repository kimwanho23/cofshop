package kwh.cofshop.chat.controller;

import jakarta.validation.Valid;
import kwh.cofshop.chat.dto.request.ChatMessageRequestDto;
import kwh.cofshop.chat.dto.request.DeleteMessageRequestDto;
import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import kwh.cofshop.chat.dto.response.DeletedMessageResponseDto;
import kwh.cofshop.chat.service.ChatMessageService;
import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@RequiredArgsConstructor
@Controller
@Validated
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate template;

    @MessageMapping("/chat.sendMessage")
    public void handleMessage(@Valid @Payload ChatMessageRequestDto requestDto, Principal principal) { // Principal을 사용해서 인증 사용자 정보를 가져온다
        Long memberId = extractMemberId(principal);
        ChatMessageResponseDto responseDto = chatMessageService.createChatMessage(requestDto, memberId);

        template.convertAndSend(
                "/topic/chatroom." + requestDto.getRoomId(),
                responseDto
        );
    }

    @MessageMapping("/chat.deleteMessage")
    public void deleteMessage(@Valid @Payload DeleteMessageRequestDto requestDto, Principal principal) {
        Long senderId = extractMemberId(principal);
        DeletedMessageResponseDto responseDto =
                chatMessageService.deleteMessage(requestDto.getRoomId(), requestDto.getMessageId(), senderId);

        template.convertAndSend(
                "/topic/chatroom." + requestDto.getRoomId(),
                responseDto
        );
    }

    private Long extractMemberId(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_INVALID);
        }
        Object principalObject = authentication.getPrincipal();
        if (!(principalObject instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_INVALID);
        }
        return userDetails.getId();
    }

}
