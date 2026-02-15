package kwh.cofshop.chat.mapper;

import kwh.cofshop.chat.domain.ChatMessage;
import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ChatMessageMapper {

    @Mapping(source = "id", target = "messageId")
    @Mapping(source = "chatRoom.id", target = "roomId")
    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(source = "messageGroupId", target = "messageGroupId")
    @Mapping(source = "messageType", target = "messageType")
    @Mapping(source = "deleted", target = "deleted")
    @Mapping(source = "createDate", target = "createdAt")
    ChatMessageResponseDto toResponseDto(ChatMessage chatMessage);
}
