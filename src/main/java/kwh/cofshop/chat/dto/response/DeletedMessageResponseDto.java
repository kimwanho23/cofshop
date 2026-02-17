package kwh.cofshop.chat.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class DeletedMessageResponseDto {
    private final Long roomId;
    private final String groupId;
    private final List<Long> messageIds;

    private DeletedMessageResponseDto(Long roomId, String groupId, List<Long> messageIds) {
        this.roomId = roomId;
        this.groupId = groupId;
        this.messageIds = messageIds;
    }

    public static DeletedMessageResponseDto of(Long roomId, String groupId, List<Long> messageIds) {
        return new DeletedMessageResponseDto(roomId, groupId, messageIds);
    }
}
