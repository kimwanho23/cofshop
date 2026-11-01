package kwh.cofshop.coupon.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.service.MemberCouponIssueService;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 쿠폰 발급 후 outbox 스케줄러가 실제 RabbitMQ 큐까지 이벤트를 전달하는지 검증
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.redis.password=",
        "spring.redis.ssl.enabled=false",
        "file.dir=build/test-uploads",
        "imp.api.secretkey=test-only",
        "coupon.rabbit.enabled=true",
        "coupon.outbox.enabled=true",
        "coupon.outbox.fixed-delay-ms=200"
})
class CouponOutboxRabbitIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("jwt.secret-key", () -> Base64.getEncoder().encodeToString(
                "test-only-key-for-coupon-outbox-rabbit-integration-test-012345".getBytes(StandardCharsets.UTF_8)));
    }

    @MockitoBean
    private PortOnePaymentClient portOnePaymentClient;

    @Autowired
    private MemberCouponIssueService memberCouponIssueService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("쿠폰 발급 이벤트가 큐에 도착하고 outbox가 발행 완료로 바뀜")
    void issuedCoupon_reachesQueue() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Member member = memberRepository.save(Member.builder()
                .email(unique + "@example.com")
                .memberName("회원")
                .memberPwd("pw")
                .tel("010" + unique)
                .build());
        Coupon coupon = couponRepository.save(Coupon.createCoupon(
                "쿠폰", 0, 100, null, CouponType.FIXED, null,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

        Long memberCouponId = memberCouponIssueService.issueCoupon(member.getId(), coupon.getId());

        Message message = rabbitTemplate.receive("coupon.issue.queue", 30_000);
        assertThat(message).isNotNull();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        JsonNode event = objectMapper.readTree(body);
        assertThat(event.get("memberCouponId").asLong()).as(body).isEqualTo(memberCouponId);
        // 발행 후 커밋하므로 메시지가 먼저 도착할 수 있음
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM coupon_issue_outbox WHERE member_coupon_id = ?", String.class, memberCouponId))
                        .isEqualTo("SENT"));
    }
}
