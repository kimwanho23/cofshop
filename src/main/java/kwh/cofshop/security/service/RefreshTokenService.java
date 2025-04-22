package kwh.cofshop.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "RT:";
    private static final long EXPIRATION = 14 * 24 * 60 * 60L; // 14일 (초)

    public void save(Long memberId, String refreshToken) {
        redisTemplate.opsForValue().set(PREFIX + memberId, refreshToken, EXPIRATION, TimeUnit.SECONDS);
    }

    public Optional<String> findByMemberId(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(PREFIX + memberId));
    }

    public boolean matches(Long memberId, String tokenToCompare) {
        String savedToken = redisTemplate.opsForValue().get(PREFIX + memberId);
        return tokenToCompare.equals(savedToken);
    }

    public void delete(Long memberId) {
        redisTemplate.delete(PREFIX + memberId);
    }
}