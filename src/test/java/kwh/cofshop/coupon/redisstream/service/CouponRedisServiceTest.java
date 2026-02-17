package kwh.cofshop.coupon.redisstream.service;

import kwh.cofshop.coupon.domain.CouponIssueState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponRedisServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private CouponRedisService couponRedisService;

    @Test
    @DisplayName("setInitCouponCount")
    void setInitCouponCount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        couponRedisService.setInitCouponCount(1L, 10);

        verify(valueOperations).set("coupon:stock:1", "10");
    }

    @Test
    @DisplayName("issueCoupon_success")
    void issueCoupon_success() {
        doReturn(List.of("success", "1"))
                .when(redisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any(Object[].class));

        CouponIssueState result = couponRedisService.issueCoupon(1L, 2L);

        assertThat(result).isEqualTo(CouponIssueState.SUCCESS);
    }

    @Test
    @DisplayName("issueCoupon_alreadyIssued")
    void issueCoupon_alreadyIssued() {
        doReturn(List.of("already_issued", "0"))
                .when(redisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any(Object[].class));

        CouponIssueState result = couponRedisService.issueCoupon(1L, 2L);

        assertThat(result).isEqualTo(CouponIssueState.ALREADY_ISSUED);
    }

    @Test
    @DisplayName("issueCoupon_outOfStock")
    void issueCoupon_outOfStock() {
        doReturn(List.of("out_of_stock", "0"))
                .when(redisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any(Object[].class));

        CouponIssueState result = couponRedisService.issueCoupon(1L, 2L);

        assertThat(result).isEqualTo(CouponIssueState.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("issueCoupon_stockNotInitialized")
    void issueCoupon_stockNotInitialized() {
        doReturn(List.of("stock_not_initialized", "0"))
                .when(redisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any(Object[].class));

        CouponIssueState result = couponRedisService.issueCoupon(1L, 2L);

        assertThat(result).isEqualTo(CouponIssueState.STOCK_NOT_INITIALIZED);
    }

    @Test
    @DisplayName("issueCoupon_unknown")
    void issueCoupon_unknown() {
        doReturn(List.of("unknown", "0"))
                .when(redisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any(Object[].class));

        assertThatThrownBy(() -> couponRedisService.issueCoupon(1L, 2L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("issueCoupon_emptyResult")
    void issueCoupon_emptyResult() {
        doReturn(null)
                .when(redisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any(Object[].class));

        assertThatThrownBy(() -> couponRedisService.issueCoupon(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty result");
    }

    @Test
    @DisplayName("hasStock")
    void hasStock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("coupon:stock:1")).thenReturn("5");

        boolean result = couponRedisService.hasStock(1L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasStock_empty")
    void hasStock_empty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("coupon:stock:1")).thenReturn(null);

        boolean result = couponRedisService.hasStock(1L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("restoreStock")
    void restoreStock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        couponRedisService.restoreStock(1L);

        verify(valueOperations).increment("coupon:stock:1");
    }

    @Test
    @DisplayName("decreaseStock_success")
    void decreaseStock_success() {
        doReturn(1L).when(redisTemplate).execute(any(DefaultRedisScript.class), anyList());

        boolean result = couponRedisService.decreaseStock(1L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("decreaseStock_failed")
    void decreaseStock_failed() {
        doReturn(0L).when(redisTemplate).execute(any(DefaultRedisScript.class), anyList());

        boolean result = couponRedisService.decreaseStock(1L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAlreadyIssued")
    void isAlreadyIssued() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("coupon:issued:1", "2")).thenReturn(true);

        boolean result = couponRedisService.isAlreadyIssued(1L, 2L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Save issued record")
    void saveIssued() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        couponRedisService.saveIssued(1L, 2L);

        verify(setOperations).add("coupon:issued:1", "2");
    }

    @Test
    @DisplayName("rollbackIssuedCoupon")
    void rollbackIssuedCoupon() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        couponRedisService.rollbackIssuedCoupon(1L, 2L);

        verify(valueOperations).increment("coupon:stock:1");
        verify(setOperations).remove("coupon:issued:1", "2");
        verify(zSetOperations).remove("coupon:issued:order:1", "2");
    }
}
