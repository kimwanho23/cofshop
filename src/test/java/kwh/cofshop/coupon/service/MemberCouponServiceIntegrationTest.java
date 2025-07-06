package kwh.cofshop.coupon.service;

import kwh.cofshop.TestSettingUtils;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import kwh.cofshop.coupon.repository.MemberCouponRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@Slf4j
class MemberCouponServiceIntegrationTest extends TestSettingUtils {

    @Autowired
    private MemberCouponService memberCouponService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private CouponService couponService;


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
    @DisplayName("수량 제한 쿠폰 발급")
    @Transactional
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void requestLimitedCouponIssue(){
        // given
        Member member = memberRepository.findById(1L).orElseThrow();

        // when: 발급 요청 (큐에 들어감)
       memberCouponService.issueCoupon(member.getId(), 47L);

       memberRepository.findById(1L).orElseThrow();
       await().atMost(Duration.ofSeconds(15));
       log.info("로직 정상 실행");
    }

    @Test
    @DisplayName("무제한 수량 쿠폰 발급")
    @Transactional
    void requestUnlimitedCouponIssue(){
        // given
        Member member = memberRepository.findById(1L).orElseThrow();

        // when: 발급 요청 (큐에 들어감)
        memberCouponService.issueCoupon(member.getId(), 45L);
    }
}