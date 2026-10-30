package kwh.cofshop;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.order.api.OrderPaymentReconcilePort;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.order.service.OrderService;
import kwh.cofshop.payment.client.portone.PortOneCancellation;
import kwh.cofshop.payment.client.portone.PortOneClientException;
import kwh.cofshop.payment.client.portone.PortOnePaymentNotFoundException;
import kwh.cofshop.payment.service.ApprovedRefundExecutor;
import kwh.cofshop.payment.service.PaymentService;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 앱 전체를 실제 MySQL, Redis로 띄워 프록시 순서(분산 락, 트랜잭션, 비동기 이벤트)까지 검증. PG만 대체
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.redis.password=",
        "spring.redis.ssl.enabled=false",
        "file.dir=build/test-uploads",
        "imp.api.secretkey=test-only",
        "coupon.stream.enabled=false",
        "coupon.rabbit.enabled=false",
        "coupon.outbox.enabled=false"
})
class ApplicationIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
        registry.add("jwt.secret-key", () -> Base64.getEncoder().encodeToString(
                "test-only-key-for-application-integration-test-0123456789".getBytes(StandardCharsets.UTF_8)));
    }

    @MockitoBean
    private PortOnePaymentClient portOnePaymentClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ApprovedRefundExecutor approvedRefundExecutor;

    @Autowired
    private OrderPaymentReconcilePort orderPaymentReconcilePort;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("스키마: 빈 DB에서 Flyway 마이그레이션만으로 엔티티 검증을 통과함")
    void flywayMigrations_appliedToEmptyDatabase() {
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class);
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL", Integer.class);

        assertThat(failed).isZero();
        assertThat(applied).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("주문 생성과 취소: 실제 락과 트랜잭션으로 재고가 차감되고 복구됨")
    void createAndCancelOrder() {
        Member buyer = saveMember();
        ItemOption option = saveOption(10);

        OrderResponseDto created = orderService.createInstanceOrder(buyer.getId(), orderRequest(option, 2, null));
        assertThat(stockOf(option)).isEqualTo(8);

        orderService.cancelOrder(buyer.getId(), created.getOrderId(), "단순 변심");

        assertThat(stockOf(option)).isEqualTo(10);
        assertThat(orderRepository.findById(created.getOrderId()).orElseThrow().getOrderState())
                .isEqualTo(OrderState.CANCELLED);
    }

    @Test
    @DisplayName("환불 승인: 커밋 후 비동기로 PG 취소까지 실행되어 주문과 결제가 완료됨")
    void approveRefund_executesRefundAsynchronously() {
        Member buyer = saveMember();
        ItemOption option = saveOption(10);
        Long orderId = savePaidOrderWithRefundRequest(buyer, option);
        PaymentEntity payment = paymentEntityRepository.findByOrderId(orderId).orElseThrow();
        String merchantUid = payment.getMerchantUid();

        when(portOnePaymentClient.cancelPayment(anyString(), any()))
                .thenReturn(new PortOneCancellation("cancel-1", "SUCCEEDED"));
        when(portOnePaymentClient.getPayment(merchantUid)).thenReturn(new PortOnePayment(
                merchantUid, "imp-" + orderId, "pg-" + orderId, "CANCELLED",
                new PortOnePayment.Amount(1000L, 1000L)
        ));

        orderService.processRefundRequestByAdmin(orderId, OrderRefundRequestStatus.APPROVED, "승인");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Order order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getOrderState()).isEqualTo(OrderState.CANCELLED);
            assertThat(order.getRefundRequestStatus()).isEqualTo(OrderRefundRequestStatus.REFUNDED);
        });
        assertThat(paymentEntityRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat(stockOf(option)).isEqualTo(10);
        verify(portOnePaymentClient, times(1)).cancelPayment(anyString(), any());
    }

    @Test
    @DisplayName("0원 주문: 포인트 전액 결제면 바로 결제 완료되고, 환불 승인 시 PG 없이 복원됨")
    void freeOrder_paidImmediatelyAndRefundedWithoutPg() {
        Member buyer = saveMember();
        transactionTemplate.executeWithoutResult(status ->
                memberRepository.findById(buyer.getId()).orElseThrow().updatePoint(5000));
        ItemOption option = saveOption(10);

        OrderResponseDto created = orderService.createInstanceOrder(buyer.getId(), orderRequest(option, 1, 1000));
        Long orderId = created.getOrderId();
        assertThat(orderRepository.findById(orderId).orElseThrow().getOrderState()).isEqualTo(OrderState.PAID);

        orderService.requestRefundRequest(buyer.getId(), orderId, "단순 변심");
        orderService.processRefundRequestByAdmin(orderId, OrderRefundRequestStatus.APPROVED, "승인");

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getOrderState()).isEqualTo(OrderState.CANCELLED);
        assertThat(order.getRefundRequestStatus()).isEqualTo(OrderRefundRequestStatus.REFUNDED);
        assertThat(stockOf(option)).isEqualTo(10);
        assertThat(memberRepository.findById(buyer.getId()).orElseThrow().getPoint()).isEqualTo(5000);
    }

    @Test
    @DisplayName("대사: 검증 요청 없이 PG 결제가 완료된 주문은 자동 취소하지 않고 결제 확정")
    void reconcile_paidWithoutVerify_confirmsInsteadOfCancelling() {
        Member buyer = saveMember();
        ItemOption option = saveOption(10);
        Long orderId = savePendingOrderWithReadyPayment(buyer, option);
        PaymentEntity payment = paymentEntityRepository.findByOrderId(orderId).orElseThrow();
        when(portOnePaymentClient.getPayment(payment.getMerchantUid())).thenReturn(pgPayment(payment, "PAID"));

        orderPaymentReconcilePort.reconcileBeforeCancel(orderId);
        boolean cancelled = orderService.autoCancelStaleUnpaidOrder(orderId, LocalDateTime.now().plusMinutes(1));

        assertThat(cancelled).isFalse();
        assertThat(orderRepository.findById(orderId).orElseThrow().getOrderState()).isEqualTo(OrderState.PAID);
        assertThat(paymentEntityRepository.findById(payment.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(stockOf(option)).isEqualTo(9);
    }

    @Test
    @DisplayName("대사: PG에 결제 건이 없으면 예정대로 자동 취소")
    void reconcile_notInPg_cancelsAsUsual() {
        Member buyer = saveMember();
        ItemOption option = saveOption(10);
        Long orderId = savePendingOrderWithReadyPayment(buyer, option);
        PaymentEntity payment = paymentEntityRepository.findByOrderId(orderId).orElseThrow();
        when(portOnePaymentClient.getPayment(payment.getMerchantUid()))
                .thenThrow(new PortOnePaymentNotFoundException("not found", new RuntimeException()));

        orderPaymentReconcilePort.reconcileBeforeCancel(orderId);
        boolean cancelled = orderService.autoCancelStaleUnpaidOrder(orderId, LocalDateTime.now().plusMinutes(1));

        assertThat(cancelled).isTrue();
        assertThat(stockOf(option)).isEqualTo(10);
    }

    @Test
    @DisplayName("대사: 주문 취소 뒤 PG 결제가 확인되면 PG 취소까지 진행")
    void reconcile_paidAfterOrderCancelled_refundsAtPg() {
        Member buyer = saveMember();
        ItemOption option = saveOption(10);
        Long orderId = savePendingOrderWithReadyPayment(buyer, option);
        PaymentEntity payment = paymentEntityRepository.findByOrderId(orderId).orElseThrow();
        when(portOnePaymentClient.getPayment(payment.getMerchantUid())).thenReturn(pgPayment(payment, "PAID"));
        assertThat(orderService.autoCancelStaleUnpaidOrder(orderId, LocalDateTime.now().plusMinutes(1))).isTrue();

        when(portOnePaymentClient.cancelPayment(anyString(), any()))
                .thenReturn(new PortOneCancellation("cancel-1", "SUCCEEDED"));
        when(portOnePaymentClient.getPayment(payment.getMerchantUid()))
                .thenReturn(pgPayment(payment, "PAID"), pgPayment(payment, "CANCELLED"));

        paymentService.reconcilePayment(payment.getId());

        verify(portOnePaymentClient, times(1)).cancelPayment(anyString(), any());
        assertThat(paymentEntityRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat(orderRepository.findById(orderId).orElseThrow().getOrderState()).isEqualTo(OrderState.CANCELLED);
    }

    @Test
    @DisplayName("환불: PG 취소 타임아웃이면 PAID로 되돌리지 않고, 재시도에서 PG 상태로 확정")
    void refund_timeout_keepsPendingThenCompletes() {
        Member buyer = saveMember();
        ItemOption option = saveOption(10);
        Long orderId = savePaidOrderWithRefundRequest(buyer, option);
        PaymentEntity payment = paymentEntityRepository.findByOrderId(orderId).orElseThrow();
        String merchantUid = payment.getMerchantUid();

        when(portOnePaymentClient.cancelPayment(anyString(), any())).thenThrow(new PortOneClientException("timeout"));
        when(portOnePaymentClient.getPayment(merchantUid)).thenReturn(pgPayment(payment, "PAID"));

        orderService.processRefundRequestByAdmin(orderId, OrderRefundRequestStatus.APPROVED, "승인");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                verify(portOnePaymentClient, atLeastOnce()).cancelPayment(anyString(), any()));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(paymentEntityRepository.findById(payment.getId()).orElseThrow().getStatus())
                        .isEqualTo(PaymentStatus.REFUND_PENDING));
        assertThat(orderRepository.findById(orderId).orElseThrow().getOrderState()).isEqualTo(OrderState.REFUND_PENDING);

        when(portOnePaymentClient.getPayment(merchantUid)).thenReturn(pgPayment(payment, "CANCELLED"));

        approvedRefundExecutor.execute(orderId);

        assertThat(paymentEntityRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat(orderRepository.findById(orderId).orElseThrow().getRefundRequestStatus())
                .isEqualTo(OrderRefundRequestStatus.REFUNDED);
    }

    private PortOnePayment pgPayment(PaymentEntity payment, String status) {
        return new PortOnePayment(
                payment.getMerchantUid(), "imp-" + payment.getOrderId(), "pg-" + payment.getOrderId(), status,
                new PortOnePayment.Amount(1000L, 1000L)
        );
    }

    private Long savePendingOrderWithReadyPayment(Member buyer, ItemOption option) {
        return transactionTemplate.execute(status -> {
            ItemOption managedOption = itemOptionRepository.findById(option.getId()).orElseThrow();
            Address address = Address.builder().city("서울").street("테헤란로").zipCode("06236").build();
            Order order = Order.createOrder(buyer, address, null, List.of(OrderItem.createOrderItem(managedOption, 1)));
            order.finalizePrice(1000L, 0, 0);
            order.pay();
            orderRepository.save(order);
            paymentEntityRepository.save(PaymentEntity.createPayment(
                    order.getId(), buyer.getId(), order.getMerchantUid(), 1000L,
                    buyer.getEmail(), buyer.getMemberName(), buyer.getTel(), "kakaopay", "card"
            ));
            return order.getId();
        });
    }

    private OrderRequestDto orderRequest(ItemOption option, int quantity, Integer usePoint) {
        OrderItemRequestDto itemRequest = new OrderItemRequestDto();
        itemRequest.setItemId(option.getItem().getId());
        itemRequest.setOptionId(option.getId());
        itemRequest.setQuantity(quantity);

        kwh.cofshop.order.dto.request.AddressRequestDto address = new kwh.cofshop.order.dto.request.AddressRequestDto();
        address.setCity("서울");
        address.setStreet("테헤란로");
        address.setZipCode("06236");

        OrderRequestDto request = new OrderRequestDto();
        request.setAddress(address);
        request.setOrderItems(List.of(itemRequest));
        request.setUsePoint(usePoint);
        return request;
    }

    private Long savePaidOrderWithRefundRequest(Member buyer, ItemOption option) {
        return transactionTemplate.execute(status -> {
            ItemOption managedOption = itemOptionRepository.findById(option.getId()).orElseThrow();
            Address address = Address.builder().city("서울").street("테헤란로").zipCode("06236").build();
            Order order = Order.createOrder(buyer, address, null, List.of(OrderItem.createOrderItem(managedOption, 1)));
            order.finalizePrice(1000L, 0, 0);
            order.changeOrderState(OrderState.PAID);
            order.requestRefundRequest("단순 변심", LocalDateTime.now());
            orderRepository.save(order);

            PaymentEntity payment = PaymentEntity.createPayment(
                    order.getId(), buyer.getId(), order.getMerchantUid(), 1000L,
                    buyer.getEmail(), buyer.getMemberName(), buyer.getTel(), "kakaopay", "card"
            );
            payment.paymentSuccess("imp-" + order.getId(), "pg-" + order.getId(), 1000L, LocalDateTime.now());
            paymentEntityRepository.save(payment);
            return order.getId();
        });
    }

    private int stockOf(ItemOption option) {
        return itemOptionRepository.findById(option.getId()).orElseThrow().getStock();
    }

    private ItemOption saveOption(int stock) {
        return transactionTemplate.execute(status -> {
            Item item = itemRepository.save(Item.builder()
                    .itemName("커피")
                    .price(1000)
                    .deliveryFee(0)
                    .origin("브라질")
                    .seller(saveMember())
                    .build());
            return itemOptionRepository.save(ItemOption.createOption("기본", 0, stock, item));
        });
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
