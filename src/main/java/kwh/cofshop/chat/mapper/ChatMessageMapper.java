package kwh.cofshop.chat.mapper;

import kwh.cofshop.chat.domain.ChatMessage;
import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ChatMessageMapper {

    ChatMessageResponseDto toResponseDto(ChatMessage chatMessage);
}
