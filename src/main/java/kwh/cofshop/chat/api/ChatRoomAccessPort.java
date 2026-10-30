package kwh.cofshop.chat.api;

public interface ChatRoomAccessPort {

    boolean existsRoom(Long roomId);

    boolean isParticipant(Long roomId, Long memberId);
}
