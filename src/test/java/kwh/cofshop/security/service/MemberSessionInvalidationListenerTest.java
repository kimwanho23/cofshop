package kwh.cofshop.security.service;

import kwh.cofshop.member.event.MemberSessionInvalidatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberSessionInvalidationListenerTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private MemberSessionInvalidationListener listener;

    @Test
    @DisplayName("회원 세션 무효화 이벤트 수신 시 리프레시 토큰을 삭제한다")
    void invalidateRefreshToken() {
        listener.invalidateRefreshToken(new MemberSessionInvalidatedEvent(1L));

        verify(refreshTokenService).delete(1L);
    }

    @Test
    @DisplayName("memberId가 없으면 리프레시 토큰을 삭제하지 않는다")
    void invalidateRefreshToken_skipWhenMemberIdMissing() {
        listener.invalidateRefreshToken(new MemberSessionInvalidatedEvent(null));

        verify(refreshTokenService, never()).delete(anyLong());
    }
}
