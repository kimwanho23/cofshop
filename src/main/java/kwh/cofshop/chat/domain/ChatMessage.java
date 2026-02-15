package kwh.cofshop.chat.domain;

import jakarta.persistence.*;
import kwh.cofshop.global.domain.BaseTimeEntity;
import kwh.cofshop.member.domain.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom; // 채팅 방 번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender; // 전송자

    @Column(nullable = false)
    private String message; // 메시지 내용

    private String messageGroupId; // UUID 기반 그룹핑

    private MessageType messageType; // 메시지 타입 구분

    @Column(nullable = false)
    private boolean isDeleted;


    @Builder
    public ChatMessage(Long id, ChatRoom chatRoom, Member sender, String message, String messageGroupId, MessageType messageType, boolean isDeleted) {
        this.id = id;
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.message = message;
        this.messageGroupId = messageGroupId;
        this.messageType = messageType;
        this.isDeleted = isDeleted;
    }

    public static ChatMessage createMessage(ChatRoom chatRoom, Member sender, String message, String messageGroupId, MessageType messageType) {
        String groupId = Optional.ofNullable(messageGroupId)
                .orElse(UUID.randomUUID().toString()); // 묶음 메시지를 그룹으로

        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .message(message)
                .messageGroupId(groupId)
                .messageType(messageType)
                .isDeleted(false)
                .build();
    }

    // 메시지 삭제(상태 변경)
    public void markAsDeleted() {
        this.isDeleted = true;
        this.message = "삭제된 메시지입니다.";
        this.messageType = MessageType.TEXT;
    }
}
