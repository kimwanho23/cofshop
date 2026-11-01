package kwh.cofshop;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.repository.MemberCouponRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.item.service.ItemService;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.dto.request.AddressRequestDto;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.order.service.OrderService;
import kwh.cofshop.payment.client.portone.PortOneCancellation;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.dto.request.PaymentRefundRequestDto;
import kwh.cofshop.payment.dto.request.PaymentVerifyRequestDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import kwh.cofshop.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// 실제 MySQL·Redis와 스프링 프록시(분산 락, 트랜잭션)로 동시 요청의 정합성을 검증. PG만 대체
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
        "coupon.rabbit.enabled=false",
        "coupon.outbox.enabled=false"
})
class ConcurrencyIntegrationTest {

    private static final int THREADS = 8;

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
                "test-only-key-for-concurrency-integration-test-0123456789".getBytes(StandardCharsets.UTF_8)));
    }

    @MockitoBean
    private PortOnePaymentClient portOnePaymentClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ItemService itemService;

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
    private CouponRepository couponRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("재고: 재고보다 많은 동시 주문은 재고만큼만 성공")
    void concurrentOrders_doNotOversell() throws Exception {
        ItemOption option = saveOption(5);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Long buyerId = saveMember().getId();
            tasks.add(() -> {
                orderService.createInstanceOrder(buyerId, orderRequest(null, null, item(option, 1)));
                return true;
            });
        }

        Outcome outcome = run(tasks);

        assertThat(outcome.unexpected).isEmpty();
        assertThat(outcome.succeeded).isEqualTo(5);
        assertThat(stockOf(option)).isZero();
    }

    @Test
    @DisplayName("재고: 여러 옵션을 반대 순서로 담은 동시 주문도 데드락 없이 완료")
    void concurrentOrders_oppositeOptionOrder_noDeadlock() throws Exception {
        ItemOption first = saveOption(100);
        ItemOption second = saveOption(100);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Long buyerId = saveMember().getId();
            boolean reversed = i % 2 == 0;
            tasks.add(() -> {
                List<OrderItemRequestDto> items = reversed
                        ? List.of(item(second, 1), item(first, 1))
                        : List.of(item(first, 1), item(second, 1));
                orderService.createInstanceOrder(buyerId, orderRequest(null, null, items.toArray(OrderItemRequestDto[]::new)));
                return true;
            });
        }

        Outcome outcome = run(tasks);

        assertThat(outcome.unexpected).isEmpty();
        assertThat(outcome.succeeded).isEqualTo(20);
        assertThat(stockOf(first)).isEqualTo(80);
        assertThat(stockOf(second)).isEqualTo(80);
    }

    @Test
    @DisplayName("재고: 관리자 재고 수정과 주문이 겹쳐도 주문 차감이 유실되지 않고 데드락이 없음")
    void adminStockUpdate_withConcurrentOrders() throws Exception {
        ItemOption option = saveOption(100);
        Long itemId = option.getItem().getId();
        Long sellerId = option.getItem().getSeller().getId();
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Long buyerId = saveMember().getId();
            tasks.add(() -> {
                orderService.createInstanceOrder(buyerId, orderRequest(null, null, item(option, 1)));
                return true;
            });
        }
        tasks.add(10, () -> {
            ItemOptionRequestDto optionDto = new ItemOptionRequestDto();
            optionDto.setId(option.getId());
            optionDto.setDescription("기본");
            optionDto.setAdditionalPrice(0);
            optionDto.setStock(200);
            optionDto.setExpectedStock(100);
            ItemUpdateRequestDto dto = new ItemUpdateRequestDto();
            dto.setExistingItemOptions(List.of(optionDto));
            itemService.updateItem(sellerId, itemId, dto, null);
            return true;
        });

        Outcome outcome = run(tasks);

        assertThat(outcome.unexpected).isEmpty();
        // 수정이 먼저면 주문이 모두 200에서 차감되고, 주문이 먼저면 수정이 거부되어 어느 차감도 사라지지 않음
        assertThat(outcome.succeeded).isIn(20, 21);
        assertThat(stockOf(option)).isEqualTo(outcome.succeeded == 21 ? 180 : 80);
    }

    @Test
    @DisplayName("포인트: 같은 회원의 동시 주문이 보유 포인트를 넘지 않음")
    void concurrentOrders_doNotOveruseBalance() throws Exception {
        Member buyer = saveMember();
        addPoint(buyer, 1000);
        ItemOption option = saveOption(100);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> {
                orderService.createInstanceOrder(buyer.getId(), orderRequest(300, null, item(option, 1)));
                return true;
            });
        }

        Outcome outcome = run(tasks);

        assertThat(outcome.unexpected).isEmpty();
        assertThat(outcome.succeeded).isEqualTo(3);
        assertThat(pointOf(buyer)).isEqualTo(100);
        assertThat(stockOf(option)).isEqualTo(97);
    }

    @Test
    @DisplayName("쿠폰: 같은 쿠폰으로 동시 주문하면 한 건만 성공")
    void concurrentOrders_sameCoupon_onlyOne() throws Exception {
        Member buyer = saveMember();
        Long memberCouponId = saveMemberCoupon(buyer);
        ItemOption option = saveOption(100);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> {
                orderService.createInstanceOrder(buyer.getId(), orderRequest(null, memberCouponId, item(option, 1)));
                return true;
            });
        }

        Outcome outcome = run(tasks);

        assertThat(outcome.unexpected).isEmpty();
        assertThat(outcome.succeeded).isEqualTo(1);
        assertThat(stockOf(option)).isEqualTo(99);
    }

    @Test
    @DisplayName("취소: 같은 주문을 동시에 취소해도 재고와 포인트는 한 번만 복원")
    void concurrentCancel_restoresOnce() throws Exception {
        Member buyer = saveMember();
        addPoint(buyer, 500);
        Long memberCouponId = saveMemberCoupon(buyer);
        ItemOption option = saveOption(10);
        OrderResponseDto order = orderService.createInstanceOrder(buyer.getId(), orderRequest(500, memberCouponId, item(option, 1)));
        assertThat(stockOf(option)).isEqualTo(9);
        assertThat(pointOf(buyer)).isZero();
        assertThat(memberCouponRepository.findById(memberCouponId).orElseThrow().getState()).isEqualTo(CouponState.USED);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> {
                orderService.cancelOrder(buyer.getId(), order.getOrderId(), "동시 취소");
                return true;
            });
        }

        Outcome outcome = run(tasks);

        assertThat(outcome.unexpected).isEmpty();
        assertThat(outcome.succeeded).isEqualTo(1);
        assertThat(stockOf(option)).isEqualTo(10);
        assertThat(pointOf(buyer)).isEqualTo(500);
        assertThat(memberCouponRepository.findById(memberCouponId).orElseThrow().getState()).isEqualTo(CouponState.AVAILABLE);
    }

    @Test
    @DisplayName("결제: 같은 결제의 검증 요청이 동시에 와도 한 번만 확정되고 오류가 없음")
    void concurrentVerify_confirmsOnce() throws Exception {
        Member buyer = saveMember();
        Long orderId = savePendingOrderWithReadyPayment(buyer, saveOption(10));
        PaymentEntity payment = paymentEntityRepository.findByOrderId(orderId).orElseThrow();
        when(portOnePaymentClient.getPayment(payment.getMerchantUid())).thenReturn(pgPayment(payment, "PAID"));

        PaymentVerifyRequestDto request = new PaymentVerifyRequestDto();
        request.setImpUid("imp-" + orderId);
        request.setMerchantUid(payment.getMerchantUid());
        request.setAmount(1000L);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> {
                paymentService.verifyPayment(buyer.getId(), payment.getId(), request);
                return true;
            });
        }

        Outcome outcome = run(tasks);

        assertThat(outcome.unexpected).isEmpty();
        assertThat(outcome.succeeded).isEqualTo(8);
        PaymentEntity confirmed = paymentEntityRepository.findById(payment.getId()).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(confirmed.getPaidAmount()).isEqualTo(1000L);
        assertThat(orderRepository.findById(orderId).orElseThrow().getOrderState()).isEqualTo(OrderState.PAID);
    }

    @Test
    @DisplayName("환불: 같은 결제의 환불 요청이 동시에 와도 재고는 한 번만 복원")
    void concurrentRefund_restoresOnce() throws Exception {
        Member buyer = saveMember();
        ItemOption option = saveOption(10);
        Long orderId = savePendingOrderWithReadyPayment(buyer, option);
        PaymentEntity payment = paymentEntityRepository.findByOrderId(orderId).orElseThrow();
        AtomicInteger cancelCalls = new AtomicInteger();
        when(portOnePaymentClient.getPayment(payment.getMerchantUid())).thenReturn(pgPayment(payment, "PAID"));

        paymentService.reconcilePayment(payment.getId());
        orderService.requestRefundRequest(buyer.getId(), orderId, "동시 환불");
        // 승인 이벤트의 자동 실행과 겹치도록 PG 취소를 성공으로 응답
        when(portOnePaymentClient.cancelPayment(anyString(), any())).thenAnswer(invocation -> {
            cancelCalls.incrementAndGet();
            return new PortOneCancellation("cancel-1", "SUCCEEDED");
        });
        when(portOnePaymentClient.getPayment(payment.getMerchantUid())).thenReturn(pgPayment(payment, "CANCELLED"));
        orderService.processRefundRequestByAdmin(orderId, OrderRefundRequestStatus.APPROVED, "승인");

        PaymentRefundRequestDto request = new PaymentRefundRequestDto();
        request.setAmount(1000L);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> {
                paymentService.refundPayment(payment.getId(), buyer.getId(), request);
                return true;
            });
        }

        Outcome outcome = run(tasks);

        assertThat(outcome.unexpected).isEmpty();
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(orderRepository.findById(orderId).orElseThrow().getRefundRequestStatus())
                        .isEqualTo(OrderRefundRequestStatus.REFUNDED));
        assertThat(paymentEntityRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat(stockOf(option)).isEqualTo(10);
        assertThat(cancelCalls).hasValue(1);
    }

    private Outcome run(List<Callable<Boolean>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Boolean> task : tasks) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();

            int succeeded = 0;
            List<Throwable> unexpected = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                try {
                    future.get(2, TimeUnit.MINUTES);
                    succeeded++;
                } catch (ExecutionException e) {
                    // 비즈니스 거절은 정상. 그 외(데드락, 락 타임아웃 등)는 결함
                    if (!(e.getCause() instanceof BusinessException)) {
                        unexpected.add(e.getCause());
                    }
                }
            }
            return new Outcome(succeeded, unexpected);
        } finally {
            executor.shutdownNow();
        }
    }

    private OrderRequestDto orderRequest(Integer usePoint, Long memberCouponId, OrderItemRequestDto... items) {
        AddressRequestDto address = new AddressRequestDto();
        address.setCity("서울");
        address.setStreet("테헤란로");
        address.setZipCode("06236");

        OrderRequestDto request = new OrderRequestDto();
        request.setAddress(address);
        request.setOrderItems(List.of(items));
        request.setUsePoint(usePoint);
        request.setMemberCouponId(memberCouponId);
        return request;
    }

    private OrderItemRequestDto item(ItemOption option, int quantity) {
        OrderItemRequestDto item = new OrderItemRequestDto();
        item.setItemId(option.getItem().getId());
        item.setOptionId(option.getId());
        item.setQuantity(quantity);
        return item;
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

    private PortOnePayment pgPayment(PaymentEntity payment, String status) {
        return new PortOnePayment(
                payment.getMerchantUid(), "imp-" + payment.getOrderId(), "pg-" + payment.getOrderId(), status,
                new PortOnePayment.Amount(1000L, 1000L)
        );
    }

    private Long saveMemberCoupon(Member buyer) {
        return transactionTemplate.execute(status -> {
            Coupon coupon = couponRepository.save(Coupon.createCoupon(
                    "쿠폰", 0, 100, null, CouponType.FIXED, null,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
            return memberCouponRepository.save(MemberCoupon.createMemberCoupon(buyer, coupon)).getId();
        });
    }

    private void addPoint(Member member, int point) {
        transactionTemplate.executeWithoutResult(status ->
                memberRepository.findById(member.getId()).orElseThrow().updatePoint(point));
    }

    private int pointOf(Member member) {
        return memberRepository.findById(member.getId()).orElseThrow().getPoint();
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

    private record Outcome(int succeeded, List<Throwable> unexpected) {
    }
}
