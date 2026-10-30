package kwh.cofshop.coupon.redisstream.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.config.QuerydslConfig;
import kwh.cofshop.config.auditing.JpaAuditingConfiguration;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponIssueState;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.messaging.CouponIssueEventPublisher;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.repository.MemberCouponRepository;
import kwh.cofshop.coupon.service.MemberCouponIssueService;
import kwh.cofshop.coupon.service.outbox.CouponIssueOutboxService;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

// Redis 선점과 DB 저장을 함께 검증. 스레드별 트랜잭션이 커밋되어야 하므로 테스트 트랜잭션을 사용하지 않음
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        QuerydslConfig.class,
        JpaAuditingConfiguration.class,
        LimitedCouponIssueService.class,
        MemberCouponIssueService.class,
        CouponIssueOutboxService.class,
        CouponRedisService.class,
        LimitedCouponIssueIntegrationTest.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LimitedCouponIssueIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @MockitoBean
    private CouponIssueEventPublisher couponIssueEventPublisher;

    @Autowired
    private LimitedCouponIssueService limitedCouponIssueService;

    @Autowired
    private CouponRedisService couponRedisService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanUp() {
        memberCouponRepository.deleteAll();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("한정 쿠폰: 동시 발급해도 DB 발급 수와 재고가 일치")
    void concurrentIssue_doesNotExceedQuantity() throws Exception {
        Coupon coupon = saveCoupon(5);
        couponRedisService.setInitCouponCount(coupon.getId(), 5);

        List<Callable<CouponIssueState>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Long memberId = saveMember().getId();
            tasks.add(() -> limitedCouponIssueService.issueCoupon(coupon.getId(), memberId));
        }

        List<CouponIssueState> results = runAll(tasks);

        assertThat(results).filteredOn(state -> state == CouponIssueState.SUCCESS).hasSize(5);
        assertThat(results).filteredOn(state -> state == CouponIssueState.OUT_OF_STOCK).hasSize(15);
        assertThat(memberCouponRepository.count()).isEqualTo(5L);
        assertThat(couponRepository.findById(coupon.getId()).orElseThrow().getCouponCount()).isZero();
        assertThat(couponRedisService.hasStock(coupon.getId())).isFalse();
    }

    @Test
    @DisplayName("한정 쿠폰: 같은 회원의 동시 요청은 한 장만 발급")
    void concurrentIssue_sameMember_onlyOne() throws Exception {
        Coupon coupon = saveCoupon(5);
        couponRedisService.setInitCouponCount(coupon.getId(), 5);
        Long memberId = saveMember().getId();

        List<Callable<CouponIssueState>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> limitedCouponIssueService.issueCoupon(coupon.getId(), memberId));
        }

        List<CouponIssueState> results = runAll(tasks);

        assertThat(results).filteredOn(state -> state == CouponIssueState.SUCCESS).hasSize(1);
        assertThat(memberCouponRepository.count()).isEqualTo(1L);
        assertThat(couponRepository.findById(coupon.getId()).orElseThrow().getCouponCount()).isEqualTo(4);
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

    private Coupon saveCoupon(int count) {
        return couponRepository.save(Coupon.createCoupon(
                "한정 쿠폰", 0, 1000, null, CouponType.FIXED, count,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)
        ));
    }

    private Member saveMember() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return memberRepository.save(Member.builder()
                .email(unique + "@example.com")
                .memberName("회원")
                .memberPwd("pw")
                .tel("010" + unique)
                .build());
    }

    @TestConfiguration
    static class Config {

        @Bean(destroyMethod = "destroy")
        LettuceConnectionFactory lettuceConnectionFactory() {
            return new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379))
            );
        }

        @Bean
        StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
            return new StringRedisTemplate(connectionFactory);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        MemberReadPort memberReadPort(MemberRepository memberRepository) {
            return new MemberReadPort() {
                @Override
                public Member getById(Long memberId) {
                    return memberRepository.findById(memberId).orElseThrow();
                }

                @Override
                public Member getByIdWithLock(Long memberId) {
                    return memberRepository.findByIdWithLock(memberId).orElseThrow();
                }

                @Override
                public Optional<Member> findByEmail(String email) {
                    return memberRepository.findByEmail(email);
                }
            };
        }
    }
}
