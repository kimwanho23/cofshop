package kwh.cofshop.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "RT:";
    private static final long EXPIRATION = 7 * 24 * 60 * 60L;

    public void save(Long memberId, String refreshToken) {
        redisTemplate.opsForValue().set(PREFIX + memberId, hashToken(refreshToken), EXPIRATION, TimeUnit.SECONDS);
    }

    public Optional<String> findByMemberId(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(PREFIX + memberId));
    }

    public boolean matches(Long memberId, String tokenToCompare) {
        String savedToken = redisTemplate.opsForValue().get(PREFIX + memberId);
        if (savedToken == null) {
            return false;
        }
        return savedToken.equals(hashToken(tokenToCompare));
    }

    public void delete(Long memberId) {
        redisTemplate.delete(PREFIX + memberId);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
