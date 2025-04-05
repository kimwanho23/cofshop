package kwh.cofshop.chat.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DeletedMessageResponseDto {
    private String groupId;
    private List<Long> messageIds;
}
