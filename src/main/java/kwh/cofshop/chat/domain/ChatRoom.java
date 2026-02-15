package kwh.cofshop.chat.domain;

import jakarta.persistence.*;
import kwh.cofshop.global.domain.BaseTimeEntity;
import kwh.cofshop.member.domain.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id; // 방 번호(식별자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Member customer; // 고객

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Member agent; // 상담사

    private ChatRoomStatus chatRoomStatus; // 채팅방 상태

    @Builder
    public ChatRoom(Long id, Member customer, Member agent, ChatRoomStatus chatRoomStatus) {
        this.id = id;
        this.customer = customer;
        this.agent = agent;
        this.chatRoomStatus = chatRoomStatus;
    }

    // 채팅방 생성
    public static ChatRoom createChatRoom(Member customer) {
        return ChatRoom.builder()
                .customer(customer)
                .agent(null) // 생성 후에 상담사를 연결해준다.
                .chatRoomStatus(ChatRoomStatus.ACTIVE)
                .build();
    }

    // 채팅 종료
    public void close() {
        this.agent = null; // 상담사 연결 해제
        this.chatRoomStatus = ChatRoomStatus.CLOSED; // 채팅방 종료
    }

    // 채팅방 참여 여부 검증
    public boolean isParticipant(Member member) {
        if (member == null || member.getId() == null) {
            return false;
        }
        Long memberId = member.getId();
        return (this.customer != null && memberId.equals(this.customer.getId()))
                || (this.agent != null && memberId.equals(this.agent.getId()));
    }

    // 채팅방이 이미 종료되었는 지
    public boolean isClosed() {
        return this.chatRoomStatus == ChatRoomStatus.CLOSED;
    }

    public boolean hasAgent() {
        return this.agent != null; // 상담사가 연결된 상태인가?
    }

    // 상담사 연결
    public void assignAgent(Member agent) {
        this.agent = agent;
        this.chatRoomStatus = ChatRoomStatus.IN_PROGRESS; // 상담 진행 중으로 변경, 이 상태에서 다른 상담사와 연결할 수 없다
    }

    public void removeAgent() {
        this.agent = null;
    }


}
