package kwh.cofshop;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.order.service.OrderService;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import kwh.cofshop.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// 실제 PortOne 테스트 결제 건으로 대사와 환불을 검증. 이 테스트는 해당 결제를 실제로 취소하며 CI에서는 실행하지 않음
@EnabledIfEnvironmentVariable(named = "PORTONE_LIVE_PAYMENT_ID", matches = ".+")
@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.redis.password=",
        "spring.redis.ssl.enabled=false",
        "file.dir=build/test-uploads",
        "coupon.rabbit.enabled=false",
        "coupon.outbox.enabled=false"
})
class PortOneLivePaymentFlowTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
        registry.add("imp.api.secretkey", () -> System.getenv("PORTONE_LIVE_SECRET"));
        registry.add("jwt.secret-key", () -> Base64.getEncoder().encodeToString(
                "test-only-key-for-portone-live-flow-test-0123456789".getBytes(StandardCharsets.UTF_8)));
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentEntityRepository paymentEntityRepository;

    @Autowired
    private ItemOptionRepository itemOptionRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("실제 PG: 검증 요청 없이 대사로 결제가 확정되고, 환불 승인으로 PG 취소까지 완료됨")
    void reconcileThenRefund() {
        String paymentId = System.getenv("PORTONE_LIVE_PAYMENT_ID");
        long amount = Long.parseLong(System.getenv("PORTONE_LIVE_AMOUNT"));
        Member buyer = saveMember();

        Long orderId = transactionTemplate.execute(status -> {
            Item item = itemRepository.save(Item.builder()
                    .itemName("커피").price((int) amount).deliveryFee(0).origin("브라질").seller(saveMember()).build());
            ItemOption option = itemOptionRepository.save(ItemOption.createOption("기본", 0, 10, item));
            Address address = Address.builder().city("서울").street("테헤란로").zipCode("06236").build();
            Order order = Order.createOrder(buyer, address, null, List.of(OrderItem.createOrderItem(option, 1)));
            order.finalizePrice(amount, 0, 0);
            order.pay();
            orderRepository.save(order);
            paymentEntityRepository.save(PaymentEntity.createPayment(
                    order.getId(), buyer.getId(), paymentId, amount,
                    buyer.getEmail(), buyer.getMemberName(), buyer.getTel(), "html5_inicis", "card"
            ));
            return order.getId();
        });
        Long paymentEntityId = paymentEntityRepository.findByOrderId(orderId).orElseThrow().getId();

        paymentService.reconcilePayment(paymentEntityId);

        PaymentEntity confirmed = paymentEntityRepository.findById(paymentEntityId).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(confirmed.getPaidAmount()).isEqualTo(amount);
        assertThat(confirmed.getImpUid()).isNotBlank();
        assertThat(orderRepository.findById(orderId).orElseThrow().getOrderState()).isEqualTo(OrderState.PAID);

        orderService.requestRefundRequest(buyer.getId(), orderId, "실연동 검증");
        orderService.processRefundRequestByAdmin(orderId, OrderRefundRequestStatus.APPROVED, "실연동 검증");

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            Order order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getOrderState()).isEqualTo(OrderState.CANCELLED);
            assertThat(order.getRefundRequestStatus()).isEqualTo(OrderRefundRequestStatus.REFUNDED);
        });
        assertThat(paymentEntityRepository.findById(paymentEntityId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
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
}
