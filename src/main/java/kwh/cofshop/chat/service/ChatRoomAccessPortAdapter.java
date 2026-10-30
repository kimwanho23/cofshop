package kwh.cofshop.chat.service;

import kwh.cofshop.chat.api.ChatRoomAccessPort;
import kwh.cofshop.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ChatRoomAccessPortAdapter implements ChatRoomAccessPort {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsRoom(Long roomId) {
        return chatRoomRepository.existsById(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isParticipant(Long roomId, Long memberId) {
        return chatRoomRepository.existsParticipant(roomId, memberId);
    }
}
