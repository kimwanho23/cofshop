package kwh.cofshop.chat.service;

import kwh.cofshop.chat.domain.ChatRoom;
import kwh.cofshop.chat.domain.ChatRoomStatus;
import kwh.cofshop.chat.dto.response.ChatRoomResponseDto;
import kwh.cofshop.chat.mapper.ChatRoomMapper;
import kwh.cofshop.chat.repository.ChatRoomRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomMapper chatRoomMapper;

        // 상담 채팅 생성
        @Transactional
        public ChatRoomResponseDto createChatRoom(Long customerId) {
            Member customer = memberRepository.findById(customerId)
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

            ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createChatRoom(customer));

            return chatRoomMapper.toResponseDto(chatRoom);
        }

        // 상담사 연결
        @Transactional
        public void joinChatAgent(Long roomId, Long agentId) {
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.BUSINESS_ERROR_CODE));

            if (chatRoom.hasAgent() || chatRoom.getChatRoomStatus() == ChatRoomStatus.IN_PROGRESS) {
                throw new BusinessException(BusinessErrorCode.BUSINESS_ERROR_CODE); // 이미 상담사 있음
            }

            Member agent = memberRepository.findById(agentId)
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.BUSINESS_ERROR_CODE));

            chatRoom.assignAgent(agent);
        }

        // 상담 채팅 종료(상태 변경)
        @Transactional
        public void closeChatRoom(Long roomId){
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.CHATROOM_NOT_FOUND));
            if (chatRoom.isClosed()) {
                throw new BusinessException(BusinessErrorCode.CHAT_ALREADY_CLOSED); // 채팅 이미 종료됨
            }
            chatRoom.close();
        }
}
