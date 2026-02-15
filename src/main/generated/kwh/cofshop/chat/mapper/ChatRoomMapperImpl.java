package kwh.cofshop.chat.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.chat.domain.ChatRoom;
import kwh.cofshop.chat.dto.response.ChatRoomResponseDto;
import kwh.cofshop.member.domain.Member;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:11+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class ChatRoomMapperImpl implements ChatRoomMapper {

    @Override
    public ChatRoomResponseDto toResponseDto(ChatRoom chatRoom) {
        if ( chatRoom == null ) {
            return null;
        }

        ChatRoomResponseDto chatRoomResponseDto = new ChatRoomResponseDto();

        chatRoomResponseDto.setRoomId( chatRoom.getId() );
        chatRoomResponseDto.setCustomerId( chatRoomCustomerId( chatRoom ) );
        Long id1 = chatRoomAgentId( chatRoom );
        if ( chatRoom.hasAgent() ) {
            chatRoomResponseDto.setAgentId( id1 );
        }

        return chatRoomResponseDto;
    }

    private Long chatRoomCustomerId(ChatRoom chatRoom) {
        if ( chatRoom == null ) {
            return null;
        }
        Member customer = chatRoom.getCustomer();
        if ( customer == null ) {
            return null;
        }
        Long id = customer.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long chatRoomAgentId(ChatRoom chatRoom) {
        if ( chatRoom == null ) {
            return null;
        }
        if ( !chatRoom.hasAgent() ) {
            return null;
        }
        Member agent = chatRoom.getAgent();
        Long id = agent.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
