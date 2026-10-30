package kwh.cofshop.config;

import kwh.cofshop.chat.api.ChatRoomAccessPort;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.ForbiddenErrorCode;
import kwh.cofshop.security.token.JwtTokenProvider;
import kwh.cofshop.security.userdetails.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long ROOM_ID = 5L;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ChatRoomAccessPort chatRoomAccessPort;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    @Test
    @DisplayName("구독: 채팅방 참여자는 허용")
    void subscribe_participant_allowed() {
        when(chatRoomAccessPort.existsRoom(ROOM_ID)).thenReturn(true);
        when(chatRoomAccessPort.isParticipant(ROOM_ID, MEMBER_ID)).thenReturn(true);
        Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/chatroom.5", authenticated());

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    @DisplayName("구독: 채팅방 비참여자는 거부")
    void subscribe_nonParticipant_denied() {
        when(chatRoomAccessPort.existsRoom(ROOM_ID)).thenReturn(true);
        when(chatRoomAccessPort.isParticipant(ROOM_ID, MEMBER_ID)).thenReturn(false);
        Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/chatroom.5", authenticated());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(ForbiddenRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ForbiddenErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/topic/*",
            "/topic/**",
            "/topic/chatroom.*",
            "/topic/chatroom.5/**",
            "/topic/other",
            "/app/chat.sendMessage"
    })
    @DisplayName("구독: 채팅방 목적지가 아니면 거부")
    void subscribe_otherDestination_denied(String destination) {
        Message<?> message = message(StompCommand.SUBSCRIBE, destination, authenticated());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(ForbiddenRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ForbiddenErrorCode.ACCESS_DENIED);
        verifyNoInteractions(chatRoomAccessPort);
    }

    @Test
    @DisplayName("구독: 로그인하지 않으면 거부")
    void subscribe_unauthenticated_denied() {
        Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/chatroom.5", null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(UnauthorizedRequestException.class);
    }

    @Test
    @DisplayName("전송: 애플리케이션 목적지는 허용")
    void send_applicationDestination_allowed() {
        Message<?> message = message(StompCommand.SEND, "/app/chat.sendMessage", authenticated());

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/topic/chatroom.5", "/topic/other", "/other"})
    @DisplayName("전송: 브로커 목적지로 직접 보내면 거부")
    void send_brokerDestination_denied(String destination) {
        Message<?> message = message(StompCommand.SEND, destination, authenticated());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(ForbiddenRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ForbiddenErrorCode.ACCESS_DENIED);
    }

    private Authentication authenticated() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        lenientId(userDetails);
        return new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
    }

    private void lenientId(CustomUserDetails userDetails) {
        org.mockito.Mockito.lenient().when(userDetails.getId()).thenReturn(MEMBER_ID);
    }

    private Message<?> message(StompCommand command, String destination, Authentication user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
