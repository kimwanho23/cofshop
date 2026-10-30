package kwh.cofshop.coupon.redisstream.service;

import kwh.cofshop.coupon.domain.CouponIssueState;
import kwh.cofshop.support.RedisIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class CouponRedisServiceIntegrationTest extends RedisIntegrationTestSupport {

    private static final Long COUPON_ID = 1L;

    private CouponRedisService couponRedisService;

    @BeforeEach
    void setUp() {
        couponRedisService = new CouponRedisService(redisTemplate());
    }

    @Test
    @DisplayName("쿠폰 Lua: 동시 발급이 재고를 넘지 않음")
    void concurrentIssue_doesNotExceedStock() throws Exception {
        couponRedisService.setInitCouponCount(COUPON_ID, 10);

        List<Callable<CouponIssueState>> tasks = new ArrayList<>();
        for (long memberId = 1; memberId <= 50; memberId++) {
            long id = memberId;
            tasks.add(() -> couponRedisService.issueCoupon(COUPON_ID, id));
        }

        List<CouponIssueState> results = runAll(tasks);

        assertThat(results).filteredOn(state -> state == CouponIssueState.SUCCESS).hasSize(10);
        assertThat(results).filteredOn(state -> state == CouponIssueState.OUT_OF_STOCK).hasSize(40);
        assertThat(couponRedisService.hasStock(COUPON_ID)).isFalse();
    }

    @Test
    @DisplayName("쿠폰 Lua: 같은 회원의 동시 요청은 한 번만 성공")
    void concurrentIssue_sameMember_onlyOnce() throws Exception {
        couponRedisService.setInitCouponCount(COUPON_ID, 10);

        List<Callable<CouponIssueState>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> couponRedisService.issueCoupon(COUPON_ID, 1L));
        }

        List<CouponIssueState> results = runAll(tasks);

        assertThat(results).filteredOn(state -> state == CouponIssueState.SUCCESS).hasSize(1);
        assertThat(results).filteredOn(state -> state == CouponIssueState.ALREADY_ISSUED).hasSize(19);
        assertThat(redisTemplate().opsForValue().get("coupon:stock:" + COUPON_ID)).isEqualTo("9");
    }

    @Test
    @DisplayName("쿠폰 Lua: 재고를 초기화하지 않으면 발급하지 않음")
    void issue_stockNotInitialized() {
        assertThat(couponRedisService.issueCoupon(COUPON_ID, 1L)).isEqualTo(CouponIssueState.STOCK_NOT_INITIALIZED);
    }

    @Test
    @DisplayName("쿠폰 Lua: 롤백하면 재고와 발급 이력이 되돌아가 다시 발급 가능")
    void rollback_restoresStockAndIssuedState() {
        couponRedisService.setInitCouponCount(COUPON_ID, 1);
        assertThat(couponRedisService.issueCoupon(COUPON_ID, 1L)).isEqualTo(CouponIssueState.SUCCESS);
        assertThat(couponRedisService.isAlreadyIssued(COUPON_ID, 1L)).isTrue();

        couponRedisService.rollbackIssuedCoupon(COUPON_ID, 1L);

        assertThat(couponRedisService.isAlreadyIssued(COUPON_ID, 1L)).isFalse();
        assertThat(couponRedisService.issueCoupon(COUPON_ID, 1L)).isEqualTo(CouponIssueState.SUCCESS);
    }

    private List<CouponIssueState> runAll(List<Callable<CouponIssueState>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<CouponIssueState> results = new ArrayList<>();
            for (Future<CouponIssueState> future : executor.invokeAll(tasks)) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdown();
        }
    }
}
