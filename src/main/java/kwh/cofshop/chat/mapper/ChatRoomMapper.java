package kwh.cofshop.chat.mapper;

import kwh.cofshop.chat.domain.ChatRoom;
import kwh.cofshop.chat.dto.response.ChatRoomResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ChatRoomMapper {

    @Mapping(source = "id", target = "roomId")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "agent.id", target = "agentId")
    ChatRoomResponseDto toResponseDto(ChatRoom chatRoom);
}
