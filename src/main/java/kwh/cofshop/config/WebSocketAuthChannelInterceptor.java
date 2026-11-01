package kwh.cofshop.config;

import kwh.cofshop.chat.api.ChatRoomAccessPort;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.ForbiddenErrorCode;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.security.token.AccessTokenRevocationStore;
import kwh.cofshop.security.token.JwtTokenProvider;
import kwh.cofshop.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_HEADER_LOWER = "authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String APPLICATION_DESTINATION_PREFIX = "/app/";
    private static final Pattern CHATROOM_DESTINATION_PATTERN = Pattern.compile("^/topic/chatroom\\.(\\d+)$");

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenRevocationStore accessTokenRevocationStore;
    private final ChatRoomAccessPort chatRoomAccessPort;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
            return message;
        }

        if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
            ensureAuthenticated(accessor);
        }

        if (StompCommand.SEND.equals(command)) {
            authorizeSend(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_NOT_EXIST);
        }
        if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isAccessToken(token)
                || accessTokenRevocationStore.isRevoked(token)) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_INVALID);
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        accessor.setUser(authentication);
    }

    private void ensureAuthenticated(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_NOT_EXIST);
        }
    }

    // 브로커(/topic)로 직접 보내면 참여자 검사와 저장을 건너뛰므로 애플리케이션 목적지만 허용
    private void authorizeSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination) || !destination.startsWith(APPLICATION_DESTINATION_PREFIX)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.ACCESS_DENIED);
        }
    }

    // 패턴 구독(/topic/*)으로 모든 방을 받을 수 있으므로 정확한 채팅방 목적지만 허용
    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.ACCESS_DENIED);
        }
        Matcher matcher = CHATROOM_DESTINATION_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.ACCESS_DENIED);
        }

        Long roomId = Long.valueOf(matcher.group(1));
        Long memberId = extractMemberId(accessor.getUser());

        if (!chatRoomAccessPort.existsRoom(roomId)) {
            throw new BusinessException(BusinessErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        if (!chatRoomAccessPort.isParticipant(roomId, memberId)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    private Long extractMemberId(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_INVALID);
        }
        Object principalObject = authentication.getPrincipal();
        if (!(principalObject instanceof CustomUserDetails userDetails) || userDetails.getId() == null) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_INVALID);
        }
        return userDetails.getId();
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(header)) {
            header = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER_LOWER);
        }
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
