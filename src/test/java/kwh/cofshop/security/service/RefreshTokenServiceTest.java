package kwh.cofshop.security.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("리프레시 토큰 저장")
    void save() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        refreshTokenService.save(1L, "token");

        verify(valueOperations).set("RT:1", hashToken("token"), 7 * 24 * 60 * 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("리프레시 토큰 조회")
    void findByMemberId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("RT:1")).thenReturn(hashToken("token"));

        Optional<String> result = refreshTokenService.findByMemberId(1L);

        assertThat(result).contains(hashToken("token"));
    }

    @Test
    @DisplayName("리프레시 토큰 조회: 없음")
    void findByMemberId_empty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("RT:1")).thenReturn(null);

        Optional<String> result = refreshTokenService.findByMemberId(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("리프레시 토큰 일치")
    void matches_true() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("RT:1")).thenReturn(hashToken("token"));

        boolean result = refreshTokenService.matches(1L, "token");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("리프레시 토큰 불일치")
    void matches_false() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("RT:1")).thenReturn(hashToken("token"));

        boolean result = refreshTokenService.matches(1L, "other");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰 삭제")
    void delete() {
        refreshTokenService.delete(1L);

        verify(redisTemplate).delete("RT:1");
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
