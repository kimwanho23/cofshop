package kwh.cofshop.chat.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.chat.domain.ChatMessage;
import kwh.cofshop.chat.dto.response.ChatMessageResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:11+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class ChatMessageMapperImpl implements ChatMessageMapper {

    @Override
    public ChatMessageResponseDto toResponseDto(ChatMessage chatMessage) {
        if ( chatMessage == null ) {
            return null;
        }

        ChatMessageResponseDto chatMessageResponseDto = new ChatMessageResponseDto();

        return chatMessageResponseDto;
    }
}
