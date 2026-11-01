package kwh.cofshop.security.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenRevocationStoreTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AccessTokenRevocationStore store;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(jwtTokenProvider.getMemberId("token")).thenReturn(1L);
        lenient().when(jwtTokenProvider.getIssuedAtMillis("token")).thenReturn(1000L);
    }

    @Test
    @DisplayName("무효화 시점은 액세스 토큰 수명보다 길게 보관")
    void revoke() {
        store.revokeIssuedBeforeNow(1L);

        verify(valueOperations).set(eq("AT-REVOKED:1"), anyString(),
                eq(JwtTokenProvider.ACCESS_TOKEN_TTL_SECONDS + 60), eq(java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("무효화 전에 발급된 토큰만 거부하고 이후 재로그인 토큰은 허용")
    void isRevoked_comparesIssuedTime() {
        when(valueOperations.get("AT-REVOKED:1")).thenReturn("1001", "1000");

        assertThat(store.isRevoked("token")).isTrue();
        assertThat(store.isRevoked("token")).isFalse();
    }

    @Test
    @DisplayName("무효화 기록이 없으면 허용")
    void isRevoked_noRecord() {
        when(valueOperations.get("AT-REVOKED:1")).thenReturn(null);

        assertThat(store.isRevoked("token")).isFalse();
    }

    @Test
    @DisplayName("회원 ID가 없는 토큰은 거부")
    void isRevoked_noMemberId() {
        when(jwtTokenProvider.getMemberId("token")).thenReturn(null);

        assertThat(store.isRevoked("token")).isTrue();
    }

    @Test
    @DisplayName("Redis 장애 때는 요청을 막지 않음")
    void isRevoked_redisFailure() {
        when(valueOperations.get("AT-REVOKED:1")).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(store.isRevoked("token")).isFalse();
    }
}
