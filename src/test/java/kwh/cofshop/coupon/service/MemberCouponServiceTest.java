package kwh.cofshop.coupon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import kwh.cofshop.TestSettingUtils;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;


@Slf4j
class MemberCouponServiceTest extends TestSettingUtils {

    @Autowired
    private MemberCouponService memberCouponService;

    @Autowired
    private MemberRepository memberRepository;


    // 스레드
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(100); // 10개의 스레드
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Test
    @DisplayName("쿠폰 발급")
    @Transactional
    void createCoupon() throws JsonProcessingException {
        Member member = createMember();
        Coupon coupon = makeCoupon();
        MemberCouponResponseDto memberCoupon = memberCouponService.createMemberCoupon(member.getId(), coupon.getId());// 쿠폰 발급
        log.info(objectMapper.writeValueAsString(memberCoupon));
    }

/*
    @Test
    @DisplayName("동시 쿠폰 발급 테스트")
    void concurrentCouponIssuanceTest() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            long memberId = i + 1300; // 각각 다른 사용자
            executor.execute(() -> {
                try {
                    memberCouponService.createMemberCoupon(memberId, 2L);
                    log.info("쿠폰 발급 : memberId={}, couponId={}", memberId, 2L);
                } catch (Exception e) {
                    log.info("발급 실패: memberId={}, 이유={}", memberId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 스레드 종료까지 대기
        executor.shutdown();
    }
*/

}