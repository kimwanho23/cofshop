package kwh.cofshop.security.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

// 탈퇴·정지·비밀번호 변경 시점 이전에 발급된 액세스 토큰을 거부한다
@Component
@RequiredArgsConstructor
@Slf4j
public class AccessTokenRevocationStore {

    private static final String PREFIX = "AT-REVOKED:";
    // 시점 기록은 그 전에 발급된 토큰이 모두 만료될 때까지만 필요
    private static final long TTL_SECONDS = JwtTokenProvider.ACCESS_TOKEN_TTL_SECONDS + 60;

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public void revokeIssuedBeforeNow(Long memberId) {
        redisTemplate.opsForValue().set(
                PREFIX + memberId, String.valueOf(System.currentTimeMillis()), TTL_SECONDS, TimeUnit.SECONDS);
    }

    // Redis 장애 때 모든 로그인 요청을 막지 않도록 허용한다. 이때 토큰은 원래 만료 시각까지 유효하다
    public boolean isRevoked(String accessToken) {
        Long memberId = jwtTokenProvider.getMemberId(accessToken);
        if (memberId == null) {
            return true;
        }
        try {
            String revokedAt = redisTemplate.opsForValue().get(PREFIX + memberId);
            return revokedAt != null
                    && jwtTokenProvider.getIssuedAtMillis(accessToken) < Long.parseLong(revokedAt);
        } catch (RuntimeException e) {
            log.warn("Access token revocation check failed. memberId={}, error={}", memberId, e.getMessage());
            return false;
        }
    }
}
