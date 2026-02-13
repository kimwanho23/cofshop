package kwh.cofshop.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kwh.cofshop.argumentResolver.LoginMember;
import kwh.cofshop.chat.dto.response.ChatRoomResponseDto;
import kwh.cofshop.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(summary = "채팅방 생성", description = "고객센터 1:1 채팅방을 생성합니다.")
    @PostMapping()
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ChatRoomResponseDto> createChatRoom(@LoginMember Long memberId) {
        ChatRoomResponseDto responseDto = chatRoomService.createChatRoom(memberId);

        return ResponseEntity
                .created(URI.create("/api/chat-rooms/" + responseDto.getRoomId()))
                .body(responseDto);
    }


    //////////// @PUT, PATCH

    // 상담사 연결 (상담사)
    @Operation(summary = "상담사 연결", description = "채팅방에 상담사를 배정합니다.")
    @PatchMapping("/{roomId}/join")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> joinChatAgent(@PathVariable Long roomId,
                                              @RequestParam Long agentId) {
        chatRoomService.joinChatAgent(roomId, agentId);
        return ResponseEntity.noContent().build();
    }

    // 상담 종료 (상담사 or 시스템)
    @Operation(summary = "상담 종료", description = "상담이 완료되었을 때, 채팅방을 종료합니다.")
    @PatchMapping("/{roomId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> closeChatRoom(@Valid @PathVariable Long roomId) {
        chatRoomService.closeChatRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    //////////// @DELETE


}
