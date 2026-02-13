package kwh.cofshop.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import kwh.cofshop.argumentResolver.LoginMember;
import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import kwh.cofshop.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-messages")
public class ChatMessageRestController {

    private final ChatMessageService chatMessageService;
    //////////// @GET

    // 채팅 메시지 목록
    @Operation(summary = "메시지 목록 조회", description = "상담 채팅방 메시지 목록을 조회합니다.")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    @GetMapping("/{roomId}/messages")
    public Slice<ChatMessageResponseDto> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "20") int pageSize,
            @LoginMember Long memberId) {
        return chatMessageService.getChatMessages(roomId, lastMessageId, pageSize, memberId);
    }
    //////////// @POST

    //////////// @PUT, PATCH

    //////////// @DELETE


}
