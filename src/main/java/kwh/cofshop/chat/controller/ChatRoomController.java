package kwh.cofshop.chat.controller;

import jakarta.validation.Valid;
import kwh.cofshop.chat.dto.response.ChatRoomResponseDto;
import kwh.cofshop.chat.service.ChatRoomService;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    //////////// @GET


    //////////// @POST

    // 채팅방 생성
    @PostMapping()
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> createChatRoom(@LoginMember CustomUserDetails customUserDetails) {
        ChatRoomResponseDto responseDto = chatRoomService.createChatRoom(customUserDetails.getId());

        return ResponseEntity
                .created(URI.create("/api/chat-rooms/" + responseDto.getRoomId()))
                .body(ApiResponse.Created(responseDto));
    }


    //////////// @PUT, PATCH

    // 상담사 연결 (상담사)
    @PatchMapping("/{roomId}/join")
    public ResponseEntity<Void> joinChatAgent(@PathVariable Long roomId,
                                              @RequestParam Long agentId) {
        chatRoomService.joinChatAgent(roomId, agentId);
        return ResponseEntity.ok().build();
    }

    // 상담 종료 (상담사 or 시스템)
    @PatchMapping("/{roomId}/close")
    public ResponseEntity<Void> closeChatRoom(@Valid @PathVariable Long roomId) {
        chatRoomService.closeChatRoom(roomId);
        return ResponseEntity.ok().build();
    }

    //////////// @DELETE


}
