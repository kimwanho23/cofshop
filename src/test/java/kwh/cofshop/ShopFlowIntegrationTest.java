package kwh.cofshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.coupon.service.outbox.CouponIssueOutboxService;
import kwh.cofshop.member.service.MemberLoginHistoryService;
import kwh.cofshop.member.service.MemberRejoinBlockService;
import kwh.cofshop.order.service.OrderWithdrawalService;
import kwh.cofshop.payment.client.portone.PortOneCancellation;
import kwh.cofshop.payment.client.portone.PortOneClientException;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 쿼리와 직렬화까지 실제로 거치는지 HTTP로 상품, 장바구니, 주문, 리뷰, 채팅 흐름을 검증
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.redis.password=",
        "spring.redis.ssl.enabled=false",
        "file.dir=build/test-uploads",
        "imp.api.secretkey=test-only",
        "coupon.rabbit.enabled=false",
        "coupon.outbox.enabled=false",
        "item.popular.cache-seconds=0"
})
class ShopFlowIntegrationTest {

    private static final String PASSWORD = "password-1234";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};

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
                "test-only-key-for-shop-flow-integration-test-0123456789012345".getBytes(StandardCharsets.UTF_8)));
    }

    @MockitoBean
    private PortOnePaymentClient portOnePaymentClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponIssueOutboxService couponIssueOutboxService;

    @Autowired
    private MemberRejoinBlockService memberRejoinBlockService;

    @Autowired
    private OrderWithdrawalService orderWithdrawalService;

    @Autowired
    private MemberLoginHistoryService memberLoginHistoryService;

    @Test
    @DisplayName("상품: 임시 이미지로 등록하고 조회, 검색, 수정, 옵션 삭제, 상품 삭제까지 동작")
    void itemLifecycle() throws Exception {
        String admin = loginAsAdmin();
        Long parentId = createCategory(admin, null);
        Long childId = createCategory(admin, parentId);
        JsonNode created = createItem(admin, childId);
        long itemId = created.get("id").asLong();

        JsonNode item = ok(HttpMethod.GET, "/api/item/" + itemId, admin, null);
        assertThat(item.get("itemOptions")).hasSize(1);
        assertThat(item.get("itemImages")).hasSize(1);
        long optionId = item.get("itemOptions").get(0).get("id").asLong();

        // 부모 카테고리로 검색해도 하위 카테고리 상품이 나옴
        JsonNode found = ok(HttpMethod.POST, "/api/item/search", null, Map.of("categoryId", parentId));
        assertThat(found.get("totalElements").asLong()).isEqualTo(1);
        assertThat(found.get("content").get(0).get("id").asLong()).isEqualTo(itemId);
        assertThat(ok(HttpMethod.GET, "/api/categories/" + parentId + "/children", admin, null)).hasSize(1);
        assertThat(ok(HttpMethod.GET, "/api/categories/" + childId + "/path", admin, null)).isNotEmpty();
        assertThat(ok(HttpMethod.GET, "/api/categories", admin, null)).isNotEmpty();

        Map<String, Object> update = Map.of(
                "itemName", "수정한 상품",
                "price", 2000,
                "deliveryFee", 0,
                "origin", "케냐",
                "itemLimit", 5,
                "existingItemOptions", List.of(Map.of(
                        "id", optionId, "description", "수정 옵션", "additionalPrice", 100, "stock", 50, "expectedStock", 10)),
                "addItemOptions", List.of(Map.of("description", "추가 옵션", "additionalPrice", 200, "stock", 5)));
        ResponseEntity<String> updated = multipart(HttpMethod.PUT, "/api/item/" + itemId, admin, update);
        assertThat(updated.getStatusCode()).as(updated.getBody()).isEqualTo(HttpStatus.OK);

        JsonNode afterUpdate = ok(HttpMethod.GET, "/api/item/" + itemId, admin, null);
        assertThat(afterUpdate.get("itemName").asText()).isEqualTo("수정한 상품");
        assertThat(afterUpdate.get("itemOptions")).hasSize(2);

        Map<String, Object> deleteOption = Map.of(
                "itemName", "수정한 상품",
                "price", 2000,
                "deliveryFee", 0,
                "origin", "케냐",
                "itemLimit", 5,
                "deleteOptionIds", List.of(optionId));
        ResponseEntity<String> optionDeleted = multipart(HttpMethod.PUT, "/api/item/" + itemId, admin, deleteOption);
        assertThat(optionDeleted.getStatusCode()).as(optionDeleted.getBody()).isEqualTo(HttpStatus.OK);
        assertThat(ok(HttpMethod.GET, "/api/item/" + itemId, admin, null).get("itemOptions")).hasSize(1);

        ResponseEntity<String> deleted = send(HttpMethod.DELETE, "/api/item/" + itemId, admin, null);
        assertThat(deleted.getStatusCode().is2xxSuccessful()).as(deleted.getBody()).isTrue();
        assertThat(send(HttpMethod.GET, "/api/item/" + itemId, admin, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("상품: 임시 이미지를 수정으로 추가·삭제하고 잘못된 입력은 400")
    void itemImagesAndValidation() throws Exception {
        String admin = loginAsAdmin();
        Long categoryId = createCategory(admin, null);
        JsonNode created = createItem(admin, categoryId);
        long itemId = created.get("id").asLong();
        long firstImageId = ok(HttpMethod.GET, "/api/item/" + itemId, admin, null).get("itemImages").get(0).get("id").asLong();
        long tempFileId = objectMapper.readTree(uploadTempImage(admin).getBody()).get(0).get("tempFileId").asLong();

        ResponseEntity<String> updated = multipart(HttpMethod.PUT, "/api/item/" + itemId, admin, Map.of(
                "itemName", "이미지 수정", "price", 1000, "deliveryFee", 0, "origin", "브라질", "itemLimit", 10,
                "addItemImgs", List.of(Map.of("tempFileId", tempFileId, "imgType", "SUB")),
                "deleteImgIds", List.of(firstImageId)));
        assertThat(updated.getStatusCode()).as(updated.getBody()).isEqualTo(HttpStatus.OK);
        JsonNode images = ok(HttpMethod.GET, "/api/item/" + itemId, admin, null).get("itemImages");
        assertThat(images).hasSize(1);
        assertThat(images.get(0).get("id").asLong()).isNotEqualTo(firstImageId);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM temp_upload_file WHERE temp_upload_file_id = ?", Integer.class, tempFileId))
                .as("상품에 쓴 임시 파일 기록 제거").isZero();

        Long newCategoryId = createCategory(admin, null);
        ResponseEntity<String> recategorized = multipart(HttpMethod.PUT, "/api/item/" + itemId, admin, Map.of(
                "itemName", "이미지 수정", "price", 1000, "deliveryFee", 0, "origin", "브라질", "itemLimit", 10,
                "addCategoryIds", List.of(newCategoryId),
                "deleteCategoryIds", List.of(categoryId)));
        assertThat(recategorized.getStatusCode()).as(recategorized.getBody()).isEqualTo(HttpStatus.OK);
        assertThat(jdbcTemplate.queryForList("SELECT category_id FROM item_category WHERE item_id = ?", Long.class, itemId))
                .containsExactly(newCategoryId);

        Map<String, Object> base = Map.of(
                "itemName", "상품", "price", 1000, "deliveryFee", 0, "origin", "브라질", "itemLimit", 10);
        Map<String, Object> imageMissing = new java.util.HashMap<>(base);
        imageMissing.put("categoryIds", List.of(categoryId));
        imageMissing.put("itemOptions", List.of(Map.of("description", "기본", "additionalPrice", 0, "stock", 1)));
        assertThat(multipart(HttpMethod.POST, "/api/item", admin, imageMissing).getStatusCode().value()).as("이미지 없음").isEqualTo(400);

        Map<String, Object> optionMissing = new java.util.HashMap<>(base);
        optionMissing.put("categoryIds", List.of(categoryId));
        optionMissing.put("itemImages", List.of(Map.of("tempFileId", 999999, "imgType", "SUB")));
        assertThat(multipart(HttpMethod.POST, "/api/item", admin, optionMissing).getStatusCode().value()).as("옵션 없음").isEqualTo(400);

        Map<String, Object> negativeStock = new java.util.HashMap<>(imageMissing);
        negativeStock.put("itemImages", List.of(Map.of("tempFileId", 999999, "imgType", "SUB")));
        negativeStock.put("itemOptions", List.of(Map.of("description", "기본", "additionalPrice", 0, "stock", -1)));
        assertThat(multipart(HttpMethod.POST, "/api/item", admin, negativeStock).getStatusCode().value()).as("음수 재고").isEqualTo(400);

        // 다른 상품의 옵션으로 장바구니와 주문을 만들 수 없음
        long[] other = createProduct(admin);
        long[] product = createProduct(admin);
        String member = signUpAndLogin();
        assertThat(send(HttpMethod.POST, "/api/cart-items/me/items", member,
                Map.of("itemId", product[0], "optionId", other[1], "quantity", 1)).getStatusCode().is4xxClientError()).as("장바구니").isTrue();
        assertThat(send(HttpMethod.POST, "/api/orders", member, Map.of(
                "address", address(),
                "orderItems", List.of(Map.of("itemId", product[0], "optionId", other[1], "quantity", 1))))
                .getStatusCode().is4xxClientError()).as("주문").isTrue();
    }

    @Test
    @DisplayName("주문: 장바구니, 주문, 주문 조회, 구매 확정 후 리뷰까지 동작")
    void cartOrderReviewFlow() throws Exception {
        String admin = loginAsAdmin();
        JsonNode item = createItem(admin, createCategory(admin, null));
        long itemId = item.get("id").asLong();
        long optionId = ok(HttpMethod.GET, "/api/item/" + itemId, admin, null).get("itemOptions").get(0).get("id").asLong();
        String member = signUpAndLogin();

        assertThat(send(HttpMethod.POST, "/api/carts/me", member, null).getStatusCode().is2xxSuccessful()).isTrue();
        ResponseEntity<String> added = send(HttpMethod.POST, "/api/cart-items/me/items", member,
                Map.of("itemId", itemId, "optionId", optionId, "quantity", 2));
        assertThat(added.getStatusCode()).as(added.getBody()).isEqualTo(HttpStatus.CREATED);
        assertThat(ok(HttpMethod.GET, "/api/cart-items/me", member, null)).hasSize(1);
        assertThat(ok(HttpMethod.GET, "/api/cart-items/me/total-price", member, null).asLong()).isEqualTo(2000L);

        ResponseEntity<String> ordered = send(HttpMethod.POST, "/api/orders", member, Map.of(
                "address", Map.of("city", "서울", "street", "테헤란로", "zipCode", "06236"),
                "orderItems", List.of(Map.of("itemId", itemId, "optionId", optionId, "quantity", 2))));
        assertThat(ordered.getStatusCode()).as(ordered.getBody()).isEqualTo(HttpStatus.CREATED);
        long orderId = objectMapper.readTree(ordered.getBody()).get("orderId").asLong();

        assertThat(ok(HttpMethod.GET, "/api/orders/" + orderId, member, null).get("orderId").asLong()).isEqualTo(orderId);
        assertThat(ok(HttpMethod.GET, "/api/orders/me", member, null).get("totalElements").asLong()).isEqualTo(1);
        assertThat(ok(HttpMethod.GET, "/api/orders", admin, null).get("totalElements").asLong()).isGreaterThanOrEqualTo(1);

        assertThat(send(HttpMethod.POST, "/api/reviews/items/" + itemId, member,
                Map.of("rating", 5, "content", "좋아요")).getStatusCode().is2xxSuccessful()).isFalse();
        jdbcTemplate.update("UPDATE orders SET order_state = 'COMPLETED' WHERE order_id = ?", orderId);

        ResponseEntity<String> reviewed = send(HttpMethod.POST, "/api/reviews/items/" + itemId, member,
                Map.of("rating", 5, "content", "좋아요"));
        assertThat(reviewed.getStatusCode()).as(reviewed.getBody()).isEqualTo(HttpStatus.CREATED);
        assertThat(ok(HttpMethod.GET, "/api/reviews/items/" + itemId, null, null).get("content")).hasSize(1);
        assertThat(ok(HttpMethod.GET, "/api/item/populars", member, null)).isNotNull();
    }

    @Test
    @DisplayName("채팅: 회원이 방을 만들고, 상담사가 입장하고, 메시지를 조회")
    void chatFlow() throws Exception {
        String admin = loginAsAdmin();
        String member = signUpAndLogin();

        ResponseEntity<String> created = send(HttpMethod.POST, "/api/chat-rooms", member, null);
        assertThat(created.getStatusCode()).as(created.getBody()).isEqualTo(HttpStatus.CREATED);
        long roomId = objectMapper.readTree(created.getBody()).get("roomId").asLong();

        Long agentId = jdbcTemplate.queryForObject(
                "SELECT MAX(member_id) FROM member WHERE role = 'ADMIN'", Long.class);
        ResponseEntity<String> joined = send(HttpMethod.PATCH,
                "/api/chat-rooms/" + roomId + "/join?agentId=" + agentId, admin, null);
        assertThat(joined.getStatusCode().is2xxSuccessful()).as(joined.getBody()).isTrue();

        assertThat(ok(HttpMethod.GET, "/api/chat-messages/" + roomId + "/messages", member, null).get("content")).isEmpty();
        assertThat(send(HttpMethod.GET, "/api/chat-messages/" + roomId + "/messages", signUpAndLogin(), null)
                .getStatusCode().is2xxSuccessful()).isFalse();
    }

    @Test
    @DisplayName("쿠폰: 생성, 발급, 주문 적용, 취소 복원, 상태 변경, 만료 처리까지 동작")
    void couponFlow() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String member = signUpAndLogin();
        String today = LocalDate.now().toString();

        ResponseEntity<String> created = send(HttpMethod.POST, "/api/coupon", admin, Map.of(
                "name", "쿠폰" + unique(), "type", "FIXED", "discountValue", 100, "minOrderPrice", 0,
                "validFrom", LocalDate.now().minusDays(1).toString(), "validTo", LocalDate.now().plusDays(10).toString()));
        assertThat(created.getStatusCode()).as(created.getBody()).isEqualTo(HttpStatus.CREATED);
        long couponId = Long.parseLong(created.getBody());
        ok(HttpMethod.GET, "/api/coupon/" + couponId, member, null);
        assertThat(ok(HttpMethod.GET, "/api/coupon", admin, null)).isNotEmpty();

        ResponseEntity<String> issued = send(HttpMethod.POST, "/api/memberCoupon/me/" + couponId, member, null);
        assertThat(issued.getStatusCode().is2xxSuccessful()).as(issued.getBody()).isTrue();
        assertThat(send(HttpMethod.POST, "/api/memberCoupon/me/" + couponId, member, null).getStatusCode().is2xxSuccessful())
                .as("같은 쿠폰 중복 발급").isFalse();
        JsonNode memberCoupons = ok(HttpMethod.GET, "/api/memberCoupon/me", member, null);
        assertThat(memberCoupons).hasSize(1);
        long memberCouponId = memberCoupons.get(0).get("memberCouponId").asLong();

        ResponseEntity<String> ordered = send(HttpMethod.POST, "/api/orders", member, Map.of(
                "address", address(),
                "memberCouponId", memberCouponId,
                "orderItems", List.of(Map.of("itemId", product[0], "optionId", product[1], "quantity", 1))));
        assertThat(ordered.getStatusCode()).as(ordered.getBody()).isEqualTo(HttpStatus.CREATED);
        long orderId = objectMapper.readTree(ordered.getBody()).get("orderId").asLong();
        assertThat(jdbcTemplate.queryForObject("SELECT final_price FROM orders WHERE order_id = ?", Long.class, orderId))
                .isEqualTo(900L);

        ResponseEntity<String> cancelled = send(HttpMethod.PATCH, "/api/orders/" + orderId + "/cancel", member,
                Map.of("cancelReason", "변심"));
        assertThat(cancelled.getStatusCode().is2xxSuccessful()).as(cancelled.getBody()).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT state FROM member_coupon WHERE member_coupon_id = ?", String.class, memberCouponId))
                .isEqualTo("AVAILABLE");

        for (String path : List.of(
                "/api/memberCoupon/expire?date=" + today,
                "/api/coupon/" + couponId + "/state?newState=EXPIRED",
                "/api/coupon/" + couponId + "/cancel")) {
            ResponseEntity<String> response = send(HttpMethod.PATCH, path, admin, null);
            assertThat(response.getStatusCode().is5xxServerError()).as(path + " " + response.getBody()).isFalse();
            assertThat(response.getStatusCode().value()).as(path + " " + response.getBody()).isNotEqualTo(400);
        }
        ResponseEntity<String> expired = send(HttpMethod.POST, "/api/coupon/expire?date=" + today, admin, null);
        assertThat(expired.getStatusCode().is2xxSuccessful()).as(expired.getBody()).isTrue();
    }

    @Test
    @DisplayName("쿠폰: 수량 제한 쿠폰은 동시 요청에도 수량만큼만 발급되고 1인 1장")
    void limitedCoupon_concurrentIssue() throws Exception {
        String admin = loginAsAdmin();
        ResponseEntity<String> created = send(HttpMethod.POST, "/api/coupon", admin, Map.of(
                "name", "선착순" + unique(), "type", "FIXED", "discountValue", 100, "minOrderPrice", 0, "couponCount", 3,
                "validFrom", LocalDate.now().minusDays(1).toString(), "validTo", LocalDate.now().plusDays(10).toString()));
        assertThat(created.getStatusCode()).as(created.getBody()).isEqualTo(HttpStatus.CREATED);
        long couponId = Long.parseLong(created.getBody());

        String sameMember = signUpAndLogin();
        List<Callable<HttpStatusCode>> requests = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String member = signUpAndLogin();
            requests.add(() -> send(HttpMethod.POST, "/api/memberCoupon/me/" + couponId, member, null).getStatusCode());
        }
        for (int i = 0; i < 2; i++) {
            requests.add(() -> send(HttpMethod.POST, "/api/memberCoupon/me/" + couponId, sameMember, null).getStatusCode());
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<HttpStatusCode> statuses = new ArrayList<>();
        try {
            for (Future<HttpStatusCode> future : executor.invokeAll(requests)) {
                statuses.add(future.get());
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(statuses).noneMatch(HttpStatusCode::is5xxServerError);
        assertThat(statuses).filteredOn(HttpStatusCode::is2xxSuccessful).hasSize(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member_coupon WHERE coupon_id = ?", Integer.class, couponId)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT coupon_count FROM coupon WHERE coupon_id = ?", Integer.class, couponId)).isZero();
    }

    @Test
    @DisplayName("주문: 결제, 배송, 환불 승인까지 이어져 재고와 포인트가 복원됨")
    void paymentShippingRefundFlow() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String memberEmail = signUp();
        String member = login(memberEmail);
        long stockBefore = stockOf(product[1]);

        Order order = payOrder(member, product);
        assertThat(orderState(order.id())).isEqualTo("PAID");
        assertThat(stockOf(product[1])).isEqualTo(stockBefore - 1);

        ok(HttpMethod.GET, "/api/payments/iamport/imp-" + order.id(), member, null);
        ok(HttpMethod.GET, "/api/admin/payments/issues", admin, null);
        ok(HttpMethod.GET, "/api/admin/payments/" + order.paymentId() + "/comparison", admin, null);

        changeState(admin, order.id(), "PREPARING_FOR_SHIPMENT");
        changeState(admin, order.id(), "SHIPPING");
        changeState(admin, order.id(), "DELIVERED");

        ResponseEntity<String> requested = send(HttpMethod.POST, "/api/orders/" + order.id() + "/refund-request", member,
                Map.of("refundRequestReason", "불량"));
        assertThat(requested.getStatusCode().is2xxSuccessful()).as(requested.getBody()).isTrue();
        assertThat(send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/confirm", member, null).getStatusCode().is2xxSuccessful())
                .as("환불 요청 중 구매 확정").isFalse();

        when(portOnePaymentClient.cancelPayment(anyString(), any())).thenReturn(new PortOneCancellation("cancel-1", "SUCCEEDED"));
        when(portOnePaymentClient.getPayment(order.merchantUid())).thenReturn(pgPayment(order, "CANCELLED"));
        ResponseEntity<String> approved = send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/refund-request", admin,
                Map.of("refundRequestStatus", "APPROVED", "processReason", "승인"));
        assertThat(approved.getStatusCode().is2xxSuccessful()).as(approved.getBody()).isTrue();

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(stockOf(product[1])).isEqualTo(stockBefore));
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(jdbcTemplate.queryForObject("SELECT status FROM payment_entity WHERE payment_id = ?", String.class, order.paymentId()))
                        .isEqualTo("CANCELLED"));
        assertThat(send(HttpMethod.POST, "/api/admin/payments/" + order.paymentId() + "/refund/retry", admin, null)
                .getStatusCode().is5xxServerError()).isFalse();
    }

    @Test
    @DisplayName("결제 실패: PG 조회 장애면 502이고 주문은 그대로이며 다시 검증하면 확정됨")
    void verify_providerDown_thenRetry() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String member = signUpAndLogin();
        Order order = prepareOrder(member, product);
        Map<String, Object> verifyRequest = Map.of(
                "impUid", "imp-" + order.id(), "merchantUid", order.merchantUid(), "amount", order.amount());

        when(portOnePaymentClient.getPayment(order.merchantUid())).thenThrow(new PortOneClientException("timeout"));
        assertThat(send(HttpMethod.POST, "/api/payments/" + order.paymentId() + "/verify", member, verifyRequest).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(paymentStatus(order)).isEqualTo("READY");

        // 관리자 비교 화면은 PG 장애에도 열려야 함
        JsonNode comparison = ok(HttpMethod.GET, "/api/admin/payments/" + order.paymentId() + "/comparison", admin, null);
        assertThat(comparison.get("providerAvailable").asBoolean()).isFalse();
        assertThat(comparison.get("consistent").asBoolean()).isFalse();
        assertThat(comparison.get("dbStatus").asText()).isEqualTo("READY");

        doReturn(pgPayment(order, "PAID")).when(portOnePaymentClient).getPayment(order.merchantUid());
        assertThat(send(HttpMethod.POST, "/api/payments/" + order.paymentId() + "/verify", member, verifyRequest)
                .getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(orderState(order.id())).isEqualTo("PAID");

        // 확정 후 다른 금액으로 다시 검증해도 상태가 바뀌지 않음
        assertThat(send(HttpMethod.POST, "/api/payments/" + order.paymentId() + "/verify", member, Map.of(
                "impUid", "imp-" + order.id(), "merchantUid", order.merchantUid(), "amount", 1L))
                .getStatusCode().is4xxClientError()).isTrue();
        assertThat(paymentStatus(order)).isEqualTo("PAID");
    }

    @Test
    @DisplayName("결제 실패: 결제 전 취소한 주문을 나중에 검증해도 주문이 되살아나지 않음")
    void verify_afterOrderCancelled() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String member = signUpAndLogin();
        Order order = prepareOrder(member, product);
        ResponseEntity<String> cancelled = send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/cancel", member, Map.of("cancelReason", "변심"));
        if (!cancelled.getStatusCode().is2xxSuccessful()) {
            // 결제 대기 건이 있으면 취소를 막는 정책인 경우 그 응답이 500이 아니어야 함
            assertThat(cancelled.getStatusCode().is4xxClientError()).as(cancelled.getBody()).isTrue();
            return;
        }

        // 취소된 주문에 돈이 들어온 경우: 주문을 되살리지 않고 PG 취소로 돌려줌
        when(portOnePaymentClient.getPayment(order.merchantUid()))
                .thenReturn(pgPayment(order, "PAID"), pgPayment(order, "CANCELLED"));
        when(portOnePaymentClient.cancelPayment(anyString(), any())).thenReturn(new PortOneCancellation("cancel-1", "SUCCEEDED"));
        ResponseEntity<String> verified = send(HttpMethod.POST, "/api/payments/" + order.paymentId() + "/verify", member, Map.of(
                "impUid", "imp-" + order.id(), "merchantUid", order.merchantUid(), "amount", order.amount()));

        assertThat(verified.getStatusCode().is5xxServerError()).as(verified.getBody()).isFalse();
        assertThat(orderState(order.id())).isEqualTo("CANCELLED");
        assertThat(paymentStatus(order)).isEqualTo("CANCELLED");
        verify(portOnePaymentClient).cancelPayment(eq(order.merchantUid()), any());
    }

    @Test
    @DisplayName("환불 실패: PG 취소 응답이 끊기면 환불 진행 상태를 유지하고, PG 취소가 확인되면 한 번만 복원됨")
    void refund_providerTimeout_thenRetryConfirms() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String member = signUpAndLogin();
        long stockBefore = stockOf(product[1]);
        Order order = payOrder(member, product);
        requestRefund(member, order);

        when(portOnePaymentClient.cancelPayment(anyString(), any())).thenThrow(new PortOneClientException("timeout"));
        approveRefund(admin, order);
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(paymentStatus(order)).isEqualTo("REFUND_PENDING"));
        assertThat(orderState(order.id())).isEqualTo("REFUND_PENDING");
        assertThat(stockOf(product[1])).isEqualTo(stockBefore - 1);

        // 실제로는 PG 취소가 성공해 있던 경우
        doReturn(pgPayment(order, "CANCELLED")).when(portOnePaymentClient).getPayment(order.merchantUid());
        ResponseEntity<String> retried = send(HttpMethod.POST, "/api/admin/payments/" + order.paymentId() + "/refund/retry", admin, null);
        assertThat(retried.getStatusCode().is2xxSuccessful()).as(retried.getBody()).isTrue();
        assertThat(paymentStatus(order)).isEqualTo("CANCELLED");
        assertThat(stockOf(product[1])).isEqualTo(stockBefore);

        ResponseEntity<String> again = send(HttpMethod.POST, "/api/admin/payments/" + order.paymentId() + "/refund/retry", admin, null);
        assertThat(again.getStatusCode().is5xxServerError()).as(again.getBody()).isFalse();
        assertThat(stockOf(product[1])).as("재시도해도 복원은 한 번").isEqualTo(stockBefore);
    }

    @Test
    @DisplayName("환불 실패: PG가 취소 실패를 응답하면 결제와 주문이 환불 전 상태로 돌아가고 다시 시도할 수 있음")
    void refund_providerRejects_rollsBack() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String member = signUpAndLogin();
        long stockBefore = stockOf(product[1]);
        Order order = payOrder(member, product);
        changeState(admin, order.id(), "PREPARING_FOR_SHIPMENT");
        requestRefund(member, order);

        when(portOnePaymentClient.cancelPayment(anyString(), any())).thenReturn(new PortOneCancellation("cancel-1", "FAILED"));
        approveRefund(admin, order);
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(paymentStatus(order)).isEqualTo("PAID");
            assertThat(orderState(order.id())).isEqualTo("PREPARING_FOR_SHIPMENT");
        });
        assertThat(stockOf(product[1])).isEqualTo(stockBefore - 1);

        doReturn(new PortOneCancellation("cancel-2", "SUCCEEDED")).when(portOnePaymentClient).cancelPayment(anyString(), any());
        doReturn(pgPayment(order, "CANCELLED")).when(portOnePaymentClient).getPayment(order.merchantUid());
        ResponseEntity<String> retried = send(HttpMethod.POST, "/api/admin/payments/" + order.paymentId() + "/refund/retry", admin, null);
        assertThat(retried.getStatusCode().is2xxSuccessful()).as(retried.getBody()).isTrue();
        assertThat(paymentStatus(order)).isEqualTo("CANCELLED");
        assertThat(stockOf(product[1])).isEqualTo(stockBefore);
    }

    @Test
    @DisplayName("환불 정책: 승인 전 환불과 사유 없는 거절을 막고, 거절 후에는 환불되지 않음")
    void refund_policy() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String member = signUpAndLogin();
        Order order = payOrder(member, product);
        Map<String, Object> refundRequest = Map.of("amount", order.amount());

        assertThat(send(HttpMethod.POST, "/api/payments/" + order.paymentId() + "/refund", member, refundRequest)
                .getStatusCode().is4xxClientError()).as("요청 없는 환불").isTrue();
        requestRefund(member, order);
        assertThat(send(HttpMethod.POST, "/api/payments/" + order.paymentId() + "/refund", member, refundRequest)
                .getStatusCode().is4xxClientError()).as("승인 전 환불").isTrue();
        assertThat(send(HttpMethod.POST, "/api/orders/" + order.id() + "/refund-request", member, Map.of("refundRequestReason", "또"))
                .getStatusCode().is4xxClientError()).as("중복 환불 요청").isTrue();
        assertThat(send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/refund-request", admin, Map.of("refundRequestStatus", "REJECTED"))
                .getStatusCode().is4xxClientError()).as("사유 없는 거절").isTrue();

        ResponseEntity<String> rejected = send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/refund-request", admin,
                Map.of("refundRequestStatus", "REJECTED", "processReason", "기간 초과"));
        assertThat(rejected.getStatusCode().is2xxSuccessful()).as(rejected.getBody()).isTrue();
        assertThat(send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/refund-request", admin,
                Map.of("refundRequestStatus", "APPROVED", "processReason", "번복")).getStatusCode().is4xxClientError())
                .as("거절 후 승인").isTrue();
        assertThat(send(HttpMethod.POST, "/api/payments/" + order.paymentId() + "/refund", member, refundRequest)
                .getStatusCode().is4xxClientError()).as("거절 후 환불").isTrue();
        assertThat(paymentStatus(order)).isEqualTo("PAID");
    }

    @Test
    @DisplayName("주문 상태: 잘못된 전환과 결제 후 고객 취소를 막음")
    void order_invalidTransitions() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String member = signUpAndLogin();
        Order order = payOrder(member, product);
        long stockAfterPay = stockOf(product[1]);

        for (String state : List.of("DELIVERED", "WAITING_FOR_PAY", "CANCELLED", "COMPLETED", "REFUND_PENDING")) {
            assertThat(send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/state", admin, Map.of("orderState", state))
                    .getStatusCode().is4xxClientError()).as("PAID -> " + state).isTrue();
            assertThat(orderState(order.id())).isEqualTo("PAID");
        }

        ResponseEntity<String> cancelPaid = send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/cancel", member, Map.of("cancelReason", "변심"));
        assertThat(cancelPaid.getStatusCode().is4xxClientError())
                .as("결제 완료 주문을 환불 없이 취소하면 돈만 받은 상태가 됨: " + cancelPaid.getBody()).isTrue();
        assertThat(stockOf(product[1])).isEqualTo(stockAfterPay);

        changeState(admin, order.id(), "PREPARING_FOR_SHIPMENT");
        changeState(admin, order.id(), "SHIPPING");
        assertThat(send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/state", admin, Map.of("orderState", "PREPARING_FOR_SHIPMENT"))
                .getStatusCode().is4xxClientError()).as("되돌리기").isTrue();
        assertThat(send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/cancel", member, Map.of("cancelReason", "변심"))
                .getStatusCode().is4xxClientError()).as("배송 중 취소").isTrue();
    }

    @Test
    @DisplayName("포인트: 포인트로 일부 결제한 주문을 환불하면 재고와 포인트가 복원됨")
    void refund_restoresPoint() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String memberEmail = signUp();
        String member = login(memberEmail);
        Long memberId = jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE email = ?", Long.class, memberEmail);
        ok(HttpMethod.PATCH, "/api/members/" + memberId + "/point?amount=300", admin, null);
        long stockBefore = stockOf(product[1]);

        ResponseEntity<String> ordered = send(HttpMethod.POST, "/api/orders", member, Map.of(
                "address", address(),
                "usePoint", 300,
                "orderItems", List.of(Map.of("itemId", product[0], "optionId", product[1], "quantity", 1))));
        assertThat(ordered.getStatusCode()).as(ordered.getBody()).isEqualTo(HttpStatus.CREATED);
        long orderId = objectMapper.readTree(ordered.getBody()).get("orderId").asLong();
        assertThat(pointOf(memberId)).isZero();
        JsonNode payment = objectMapper.readTree(send(HttpMethod.POST, "/api/payments/orders/" + orderId, member,
                Map.of("pgProvider", "kakaopay", "payMethod", "card")).getBody());
        Order order = new Order(orderId, payment.get("paymentId").asLong(), payment.get("merchantUid").asText(), payment.get("price").asLong());
        assertThat(order.amount()).isEqualTo(700L);
        when(portOnePaymentClient.getPayment(order.merchantUid())).thenReturn(pgPayment(order, "PAID"));
        assertThat(send(HttpMethod.POST, "/api/payments/" + order.paymentId() + "/verify", member, Map.of(
                "impUid", "imp-" + orderId, "merchantUid", order.merchantUid(), "amount", 700L)).getStatusCode().is2xxSuccessful()).isTrue();

        requestRefund(member, order);
        when(portOnePaymentClient.cancelPayment(anyString(), any())).thenReturn(new PortOneCancellation("cancel-1", "SUCCEEDED"));
        doReturn(pgPayment(order, "CANCELLED")).when(portOnePaymentClient).getPayment(order.merchantUid());
        approveRefund(admin, order);

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(paymentStatus(order)).isEqualTo("CANCELLED");
            assertThat(stockOf(product[1])).isEqualTo(stockBefore);
            assertThat(pointOf(memberId)).isEqualTo(300);
        });
    }

    @Test
    @DisplayName("주문: 구매 확정 후 리뷰를 수정·삭제하고 판매 통계에 집계됨")
    void confirmReviewStatisticsFlow() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String member = signUpAndLogin();
        Order order = payOrder(member, product);

        assertThat(send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/confirm", member, null).getStatusCode().is2xxSuccessful())
                .as("배송 전 구매 확정").isFalse();
        assertThat(send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/state", admin, Map.of("orderState", "DELIVERED"))
                .getStatusCode().is5xxServerError()).isFalse();
        changeState(admin, order.id(), "PREPARING_FOR_SHIPMENT");
        changeState(admin, order.id(), "SHIPPING");
        changeState(admin, order.id(), "DELIVERED");
        ResponseEntity<String> confirmed = send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/confirm", member, null);
        assertThat(confirmed.getStatusCode().is2xxSuccessful()).as(confirmed.getBody()).isTrue();
        assertThat(orderState(order.id())).isEqualTo("COMPLETED");

        ResponseEntity<String> reviewed = send(HttpMethod.POST, "/api/reviews/items/" + product[0], member,
                Map.of("rating", 5, "content", "좋아요"));
        assertThat(reviewed.getStatusCode()).as(reviewed.getBody()).isEqualTo(HttpStatus.CREATED);
        long reviewId = objectMapper.readTree(reviewed.getBody()).get("reviewId").asLong();
        ResponseEntity<String> edited = send(HttpMethod.PUT, "/api/reviews/" + reviewId, member,
                Map.of("rating", 4, "content", "괜찮아요"));
        assertThat(edited.getStatusCode().is2xxSuccessful()).as(edited.getBody()).isTrue();
        assertThat(send(HttpMethod.PUT, "/api/reviews/" + reviewId, signUpAndLogin(), Map.of("rating", 1, "content", "x"))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> reviewDeleted = send(HttpMethod.DELETE, "/api/reviews/" + reviewId, member, null);
        assertThat(reviewDeleted.getStatusCode().is2xxSuccessful()).as(reviewDeleted.getBody()).isTrue();

        String today = LocalDate.now().toString();
        assertThat(ok(HttpMethod.GET, "/api/statistics/daily-sales?date=" + today, admin, null)).isNotEmpty();
        assertThat(ok(HttpMethod.GET, "/api/item/populars?limit=5", member, null)).isNotEmpty();
    }

    @Test
    @DisplayName("결제: 주문을 동시에 처리하지 않아도 관리자 대사로 결제가 확정됨")
    void adminReconcileFlow() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String member = signUpAndLogin();
        Order order = prepareOrder(member, product);
        when(portOnePaymentClient.getPayment(order.merchantUid())).thenReturn(pgPayment(order, "PAID"));

        ResponseEntity<String> reconciled = send(HttpMethod.POST, "/api/admin/payments/" + order.paymentId() + "/reconcile", admin, null);
        assertThat(reconciled.getStatusCode().is2xxSuccessful()).as(reconciled.getBody()).isTrue();
        assertThat(orderState(order.id())).isEqualTo("PAID");
    }

    @Test
    @DisplayName("장바구니: 수정과 삭제, 대량 추가가 동작")
    void cartManagement() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        long[] other = createProduct(admin);
        String member = signUpAndLogin();

        assertThat(send(HttpMethod.POST, "/api/carts/me", member, null).getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/carts/me/exists", member, null).asBoolean()).isTrue();
        ResponseEntity<String> bulk = send(HttpMethod.POST, "/api/cart-items/me/items/list", member, List.of(
                Map.of("itemId", product[0], "optionId", product[1], "quantity", 1),
                Map.of("itemId", other[0], "optionId", other[1], "quantity", 2)));
        assertThat(bulk.getStatusCode().is2xxSuccessful()).as(bulk.getBody()).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/cart-items/me/total-price", member, null).asLong()).isEqualTo(3000L);

        ResponseEntity<String> updated = send(HttpMethod.PATCH, "/api/cart-items/me/quantity", member,
                Map.of("itemId", product[0], "optionId", product[1], "quantity", 3));
        assertThat(updated.getStatusCode().is2xxSuccessful()).as(updated.getBody()).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/cart-items/me/total-price", member, null).asLong()).isEqualTo(5000L);

        ResponseEntity<String> oneDeleted = send(HttpMethod.DELETE, "/api/cart-items/me/items/" + other[1], member, null);
        assertThat(oneDeleted.getStatusCode().is2xxSuccessful()).as(oneDeleted.getBody()).isTrue();
        ResponseEntity<String> allDeleted = send(HttpMethod.DELETE, "/api/cart-items/me/items", member, null);
        assertThat(allDeleted.getStatusCode().is2xxSuccessful()).as(allDeleted.getBody()).isTrue();
        assertThat(ok(HttpMethod.GET, "/api/cart-items/me", member, null)).isEmpty();
        ResponseEntity<String> cartDeleted = send(HttpMethod.DELETE, "/api/carts/me", member, null);
        assertThat(cartDeleted.getStatusCode().is2xxSuccessful()).as(cartDeleted.getBody()).isTrue();
    }

    @Test
    @DisplayName("관리: 회원, 카테고리, 채팅 종료, 판매 상태 API와 경계값이 500이 아님")
    void adminApisAndBoundaries() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String memberEmail = signUp();
        String member = login(memberEmail);
        Long memberId = jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE email = ?", Long.class, memberEmail);

        ok(HttpMethod.GET, "/api/members", admin, null);
        ok(HttpMethod.GET, "/api/members/" + memberId, member, null);
        assertThat(ok(HttpMethod.GET, "/api/members/history/" + memberId, admin, null)).isNotNull();
        ok(HttpMethod.PATCH, "/api/members/" + memberId + "/point?amount=500", admin, null);
        assertThat(send(HttpMethod.PATCH, "/api/members/" + memberId + "/point?amount=-100000", admin, null)
                .getStatusCode().is4xxClientError()).as("포인트 초과 차감").isTrue();

        Long parent = createCategory(admin, null);
        Long child = createCategory(admin, parent);
        assertThat(send(HttpMethod.DELETE, "/api/categories/" + parent, admin, null).getStatusCode().is2xxSuccessful())
                .as("하위가 있는 카테고리 삭제").isFalse();
        ResponseEntity<String> categoryDeleted = send(HttpMethod.DELETE, "/api/categories/" + child, admin, null);
        assertThat(categoryDeleted.getStatusCode().is2xxSuccessful()).as(categoryDeleted.getBody()).isTrue();

        ResponseEntity<String> stopped = send(HttpMethod.PATCH, "/api/item/" + product[0] + "/state", admin, Map.of("itemState", "STOP"));
        assertThat(stopped.getStatusCode().is2xxSuccessful()).as(stopped.getBody()).isTrue();
        assertThat(send(HttpMethod.POST, "/api/orders", member, Map.of(
                "address", address(),
                "orderItems", List.of(Map.of("itemId", product[0], "optionId", product[1], "quantity", 1))))
                .getStatusCode().is4xxClientError()).as("판매 중지 상품 주문").isTrue();

        long roomId = objectMapper.readTree(send(HttpMethod.POST, "/api/chat-rooms", member, null).getBody()).get("roomId").asLong();
        ResponseEntity<String> closed = send(HttpMethod.PATCH, "/api/chat-rooms/" + roomId + "/close", admin, null);
        assertThat(closed.getStatusCode().is2xxSuccessful()).as(closed.getBody()).isTrue();

        for (String path : List.of(
                "/api/item/populars?limit=0",
                "/api/item/populars?limit=-1",
                "/api/item/populars?limit=100000",
                "/api/chat-messages/" + roomId + "/messages?pageSize=0",
                "/api/chat-messages/" + roomId + "/messages?pageSize=-1",
                "/api/chat-messages/" + roomId + "/messages?pageSize=1000000",
                "/api/orders/me?page=-1&size=-1",
                "/api/orders/me?page=0&size=1000000",
                "/api/item/999999999",
                "/api/orders/999999999",
                "/api/categories/999999999/path",
                "/api/categories/999999999/children",
                "/api/coupon/999999999",
                "/api/reviews/items/999999999")) {
            ResponseEntity<String> response = send(HttpMethod.GET, path, member, null);
            assertThat(response.getStatusCode().is5xxServerError()).as(path + " " + response.getBody()).isFalse();
        }
        for (String badSort : List.of("sortBy", "없는필드")) {
            ResponseEntity<String> response = send(HttpMethod.POST, "/api/item/search", null, Map.of("sortBy", badSort, "size", 5));
            assertThat(response.getStatusCode().is5xxServerError()).as(response.getBody()).isFalse();
        }
    }

    @Test
    @DisplayName("비로그인 고객도 상품 상세, 카테고리, 리뷰, 검색을 볼 수 있음")
    void anonymousCanBrowse() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);

        ok(HttpMethod.GET, "/api/item/" + product[0], null, null);
        ok(HttpMethod.GET, "/api/item/populars", null, null);
        ok(HttpMethod.GET, "/api/categories", null, null);
        ok(HttpMethod.GET, "/api/reviews/items/" + product[0], null, null);
        ok(HttpMethod.POST, "/api/item/search", null, Map.of("itemName", "상품"));

        // 변경 API는 비로그인으로 열리면 안 됨
        assertThat(send(HttpMethod.POST, "/api/reviews/items/" + product[0], null, Map.of("rating", 5, "content", "x"))
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(send(HttpMethod.DELETE, "/api/item/" + product[0], null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(send(HttpMethod.POST, "/api/categories", null, Map.of("name", "x")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("로그인 이력: 최근 100건만 최신순으로 응답")
    void loginHistory_limitedToRecent() throws Exception {
        String admin = loginAsAdmin();
        String email = signUp();
        Long memberId = jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE email = ?", Long.class, email);
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        for (int i = 0; i < 105; i++) {
            jdbcTemplate.update("INSERT INTO member_login_history (member_id, login_dt, ip_address, device) VALUES (?, ?, ?, ?)",
                    memberId, base.plusMinutes(i), "127.0.0.1", "device-" + i);
        }

        JsonNode history = ok(HttpMethod.GET, "/api/members/history/" + memberId, admin, null);

        assertThat(history).hasSize(100);
        assertThat(history.get(0).get("device").asText()).isEqualTo("device-104");
        assertThat(history.get(99).get("device").asText()).isEqualTo("device-5");
    }

    @Test
    @DisplayName("outbox 정리: 기준 이전의 발행 완료 행만 지우고 최근 행과 실패 행은 남김")
    void outboxCleanup_deletesOnlyOldSent() {
        LocalDateTime now = LocalDateTime.now();
        long oldSent = insertOutbox("SENT", now.minusDays(8));
        long recentSent = insertOutbox("SENT", now.minusDays(1));
        long oldFailed = insertOutbox("FAILED", null);
        long pending = insertOutbox("PENDING", null);

        int deleted = couponIssueOutboxService.deleteSentEventsBefore(now.minusDays(7), 1000);

        assertThat(deleted).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT coupon_issue_outbox_id FROM coupon_issue_outbox", Long.class))
                .doesNotContain(oldSent)
                .contains(recentSent, oldFailed, pending);
    }

    @Test
    @DisplayName("구매 확정 후 환불: PG 실패 시 구매 확정으로 돌아가고, 성공하면 재고가 복원되며, 배송 3개월 뒤에는 거부")
    void refund_afterPurchaseConfirmation() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String member = signUpAndLogin();
        long stockBefore = stockOf(product[1]);
        Order order = completedOrder(admin, member, product);

        requestRefund(member, order);
        when(portOnePaymentClient.cancelPayment(anyString(), any())).thenReturn(new PortOneCancellation("cancel-1", "FAILED"));
        approveRefund(admin, order);
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            // 환불 시도 전 상태와 구분되도록 PG 취소 호출까지 확인
            org.mockito.Mockito.verify(portOnePaymentClient, org.mockito.Mockito.atLeastOnce()).cancelPayment(org.mockito.ArgumentMatchers.eq(order.merchantUid()), any());
            assertThat(paymentStatus(order)).isEqualTo("PAID");
            assertThat(orderState(order.id())).isEqualTo("COMPLETED");
        });

        doReturn(new PortOneCancellation("cancel-2", "SUCCEEDED")).when(portOnePaymentClient).cancelPayment(anyString(), any());
        doReturn(pgPayment(order, "CANCELLED")).when(portOnePaymentClient).getPayment(order.merchantUid());
        ResponseEntity<String> retried = send(HttpMethod.POST, "/api/admin/payments/" + order.paymentId() + "/refund/retry", admin, null);
        assertThat(retried.getStatusCode().is2xxSuccessful()).as(retried.getBody()).isTrue();
        assertThat(paymentStatus(order)).isEqualTo("CANCELLED");
        assertThat(orderState(order.id())).isEqualTo("CANCELLED");
        assertThat(stockOf(product[1])).isEqualTo(stockBefore);

        Order expired = completedOrder(admin, member, product);
        jdbcTemplate.update("UPDATE orders SET delivered_at = ? WHERE order_id = ?", LocalDateTime.now().minusMonths(4), expired.id());
        assertThat(send(HttpMethod.POST, "/api/orders/" + expired.id() + "/refund-request", member, Map.of("refundRequestReason", "늦음"))
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("탈퇴: 진행 중 주문이 있으면 거부하고, 끝나면 회원 정보를 지우며 거래 개인정보를 분리 보관")
    void withdrawal_erasesAndArchives() throws Exception {
        String admin = loginAsAdmin();
        long[] product = createProduct(admin);
        String email = signUp();
        String member = login(email);
        Long memberId = jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE email = ?", Long.class, email);
        Order order = payOrder(member, product);

        ResponseEntity<String> blocked = send(HttpMethod.PATCH, "/api/members/me/state", member, null);
        assertThat(blocked.getStatusCode()).as(blocked.getBody()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM member WHERE member_id = ?", String.class, memberId)).isEqualTo(email);

        changeState(admin, order.id(), "PREPARING_FOR_SHIPMENT");
        changeState(admin, order.id(), "SHIPPING");
        changeState(admin, order.id(), "DELIVERED");
        assertThat(send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/confirm", member, null).getStatusCode().is2xxSuccessful()).isTrue();
        Order old = completedOrder(admin, member, product);
        jdbcTemplate.update("UPDATE orders SET order_date = ? WHERE order_id = ?", LocalDateTime.now().minusYears(6), old.id());

        ResponseEntity<String> withdrawn = send(HttpMethod.PATCH, "/api/members/me/state", member, null);
        assertThat(withdrawn.getStatusCode()).as(withdrawn.getBody()).isEqualTo(HttpStatus.NO_CONTENT);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM member WHERE member_id = ?", memberId);
        assertThat(row.get("email")).isEqualTo("withdrawn-" + memberId + "@deleted.invalid");
        assertThat(row.get("member_name")).isEqualTo("탈퇴회원");
        assertThat(row.get("tel")).isEqualTo("-");
        assertThat(row.get("member_pwd")).isEqualTo("!");
        assertThat(row.get("member_state")).isEqualTo("QUIT");

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE member_id = ? AND (city IS NOT NULL OR street IS NOT NULL OR zip_code IS NOT NULL OR delivery_request IS NOT NULL)", Integer.class, memberId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment_entity WHERE member_id = ? AND (buyer_email IS NOT NULL OR buyer_name IS NOT NULL OR buyer_tel IS NOT NULL)", Integer.class, memberId))
                .isZero();
        Map<String, Object> archive = jdbcTemplate.queryForMap("SELECT * FROM order_personal_archive WHERE order_id = ?", order.id());
        assertThat(archive.get("buyer_email")).isEqualTo(email);
        assertThat(archive.get("city")).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_personal_archive WHERE order_id = ?", Integer.class, old.id()))
                .as("이미 5년이 지난 주문은 보관하지 않음").isZero();

        assertThat(send(HttpMethod.POST, "/api/auth/login", null, Map.of("email", email, "memberPwd", PASSWORD)).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(send(HttpMethod.PATCH, "/api/members/" + memberId + "/state?memberState=ACTIVE", admin, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(ok(HttpMethod.GET, "/api/orders", admin, null)).as("배송지가 지워진 주문도 관리자 목록에서 조회됨").isNotNull();

        jdbcTemplate.update("UPDATE order_personal_archive SET expires_at = ? WHERE order_id = ?", LocalDateTime.now().minusDays(1), order.id());
        assertThat(orderWithdrawalService.deleteExpiredArchives()).isGreaterThanOrEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_personal_archive WHERE order_id = ?", Integer.class, order.id())).isZero();
    }

    @Test
    @DisplayName("로그인 이력: 보관 기간이 지난 행만 삭제")
    void loginHistory_retention() {
        long memberId = System.nanoTime();
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(3);
        jdbcTemplate.update("INSERT INTO member_login_history (member_id, login_dt, device) VALUES (?, ?, 'old')", memberId, cutoff.minusDays(1));
        jdbcTemplate.update("INSERT INTO member_login_history (member_id, login_dt, device) VALUES (?, ?, 'new')", memberId, cutoff.plusDays(1));

        assertThat(memberLoginHistoryService.deleteLoginHistoryBefore(cutoff, 1000)).isGreaterThanOrEqualTo(1);

        assertThat(jdbcTemplate.queryForList("SELECT device FROM member_login_history WHERE member_id = ?", String.class, memberId))
                .containsExactly("new");
    }

    @Test
    @DisplayName("재가입 제한: 탈퇴한 이메일과 전화번호는 기간 동안 거부하고, 기간이 지나면 가입할 수 있음")
    void rejoin_restrictedUntilExpiry() throws Exception {
        String unique = unique();
        String email = unique + "@example.com";
        String tel = uniqueTel();
        assertThat(send(HttpMethod.POST, "/api/members/signup", null, Map.of(
                "email", email, "memberName", "회원" + unique, "memberPwd", PASSWORD, "tel", tel)).getStatusCode().is2xxSuccessful()).isTrue();
        String member = login(email);
        assertThat(send(HttpMethod.PATCH, "/api/members/me/state", member, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> sameEmail = send(HttpMethod.POST, "/api/members/signup", null, Map.of(
                "email", email.toUpperCase(), "memberName", "회원" + unique, "memberPwd", PASSWORD, "tel", uniqueTel()));
        assertThat(sameEmail.getStatusCode()).as(sameEmail.getBody()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sameEmail.getBody()).contains("MEMBER-REJOIN-RESTRICTED");
        assertThat(send(HttpMethod.POST, "/api/members/signup", null, Map.of(
                "email", unique() + "@example.com", "memberName", "회원" + unique, "memberPwd", PASSWORD, "tel", tel)).getStatusCode())
                .as("같은 전화번호").isEqualTo(HttpStatus.CONFLICT);

        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member_rejoin_block WHERE identifier_hash LIKE ?", Integer.class, "%" + unique + "%");
        assertThat(rows).as("원문이 저장되지 않음").isZero();

        jdbcTemplate.update("UPDATE member_rejoin_block SET expires_at = ?", LocalDateTime.now().minusDays(1));
        ResponseEntity<String> rejoined = send(HttpMethod.POST, "/api/members/signup", null, Map.of(
                "email", email, "memberName", "회원" + unique, "memberPwd", PASSWORD, "tel", tel));
        assertThat(rejoined.getStatusCode().is2xxSuccessful()).as(rejoined.getBody()).isTrue();
        assertThat(memberRejoinBlockService.deleteExpired()).isGreaterThanOrEqualTo(2);
    }

    private Order completedOrder(String admin, String member, long[] product) throws Exception {
        Order order = payOrder(member, product);
        changeState(admin, order.id(), "PREPARING_FOR_SHIPMENT");
        changeState(admin, order.id(), "SHIPPING");
        changeState(admin, order.id(), "DELIVERED");
        ResponseEntity<String> confirmed = send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/confirm", member, null);
        assertThat(confirmed.getStatusCode().is2xxSuccessful()).as(confirmed.getBody()).isTrue();
        return order;
    }

    private long insertOutbox(String status, LocalDateTime publishedAt) {
        jdbcTemplate.update("""
                        INSERT INTO coupon_issue_outbox
                            (member_coupon_id, member_id, coupon_id, status, payload, retry_count, published_at)
                        VALUES (1, 1, 1, ?, '{}', 0, ?)
                        """,
                status, publishedAt);
        return jdbcTemplate.queryForObject("SELECT MAX(coupon_issue_outbox_id) FROM coupon_issue_outbox", Long.class);
    }

    private record Order(long id, long paymentId, String merchantUid, long amount) {
    }

    private Order payOrder(String member, long[] product) throws Exception {
        Order order = prepareOrder(member, product);
        when(portOnePaymentClient.getPayment(order.merchantUid())).thenReturn(pgPayment(order, "PAID"));
        ResponseEntity<String> verified = send(HttpMethod.POST, "/api/payments/" + order.paymentId() + "/verify", member, Map.of(
                "impUid", "imp-" + order.id(), "merchantUid", order.merchantUid(), "amount", order.amount()));
        assertThat(verified.getStatusCode().is2xxSuccessful()).as(verified.getBody()).isTrue();
        return order;
    }

    private Order prepareOrder(String member, long[] product) throws Exception {
        ResponseEntity<String> ordered = send(HttpMethod.POST, "/api/orders", member, Map.of(
                "address", address(),
                "orderItems", List.of(Map.of("itemId", product[0], "optionId", product[1], "quantity", 1))));
        assertThat(ordered.getStatusCode()).as(ordered.getBody()).isEqualTo(HttpStatus.CREATED);
        long orderId = objectMapper.readTree(ordered.getBody()).get("orderId").asLong();

        ResponseEntity<String> prepared = send(HttpMethod.POST, "/api/payments/orders/" + orderId, member,
                Map.of("pgProvider", "kakaopay", "payMethod", "card"));
        assertThat(prepared.getStatusCode().is2xxSuccessful()).as(prepared.getBody()).isTrue();
        JsonNode payment = objectMapper.readTree(prepared.getBody());
        return new Order(orderId, payment.get("paymentId").asLong(), payment.get("merchantUid").asText(), payment.get("price").asLong());
    }

    private void requestRefund(String member, Order order) {
        ResponseEntity<String> requested = send(HttpMethod.POST, "/api/orders/" + order.id() + "/refund-request", member,
                Map.of("refundRequestReason", "불량"));
        assertThat(requested.getStatusCode().is2xxSuccessful()).as(requested.getBody()).isTrue();
    }

    private void approveRefund(String admin, Order order) {
        ResponseEntity<String> approved = send(HttpMethod.PATCH, "/api/orders/" + order.id() + "/refund-request", admin,
                Map.of("refundRequestStatus", "APPROVED", "processReason", "승인"));
        assertThat(approved.getStatusCode().is2xxSuccessful()).as(approved.getBody()).isTrue();
    }

    private String paymentStatus(Order order) {
        return jdbcTemplate.queryForObject("SELECT status FROM payment_entity WHERE payment_id = ?", String.class, order.paymentId());
    }

    private int pointOf(Long memberId) {
        return jdbcTemplate.queryForObject("SELECT point FROM member WHERE member_id = ?", Integer.class, memberId);
    }

    private PortOnePayment pgPayment(Order order, String status) {
        return new PortOnePayment(order.merchantUid(), "imp-" + order.id(), "pg-" + order.id(), status,
                new PortOnePayment.Amount(order.amount(), order.amount()));
    }

    private void changeState(String admin, long orderId, String state) {
        ResponseEntity<String> response = send(HttpMethod.PATCH, "/api/orders/" + orderId + "/state", admin, Map.of("orderState", state));
        assertThat(response.getStatusCode().is2xxSuccessful()).as(state + " " + response.getBody()).isTrue();
        assertThat(orderState(orderId)).isEqualTo(state);
    }

    private String orderState(long orderId) {
        return jdbcTemplate.queryForObject("SELECT order_state FROM orders WHERE order_id = ?", String.class, orderId);
    }

    private long stockOf(long optionId) {
        return jdbcTemplate.queryForObject("SELECT stock FROM item_option WHERE item_option_id = ?", Long.class, optionId);
    }

    private Map<String, String> address() {
        return Map.of("city", "서울", "street", "테헤란로", "zipCode", "06236");
    }

    // {상품 ID, 옵션 ID}
    private long[] createProduct(String admin) throws Exception {
        long itemId = createItem(admin, createCategory(admin, null)).get("id").asLong();
        long optionId = ok(HttpMethod.GET, "/api/item/" + itemId, admin, null).get("itemOptions").get(0).get("id").asLong();
        return new long[]{itemId, optionId};
    }

    private Long createCategory(String admin, Long parentId) throws Exception {
        Map<String, Object> body = parentId == null
                ? Map.of("name", "카테고리" + unique())
                : Map.of("name", "카테고리" + unique(), "parentCategoryId", parentId);
        ResponseEntity<String> response = send(HttpMethod.POST, "/api/categories", admin, body);
        assertThat(response.getStatusCode().is2xxSuccessful()).as(response.getBody()).isTrue();
        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }

    private JsonNode createItem(String admin, Long categoryId) throws Exception {
        ResponseEntity<String> uploaded = uploadTempImage(admin);
        assertThat(uploaded.getStatusCode()).as(uploaded.getBody()).isEqualTo(HttpStatus.OK);
        long tempFileId = objectMapper.readTree(uploaded.getBody()).get(0).get("tempFileId").asLong();

        ResponseEntity<String> created = multipart(HttpMethod.POST, "/api/item", admin, Map.of(
                "itemName", "상품" + unique(),
                "price", 1000,
                "deliveryFee", 0,
                "origin", "브라질",
                "itemLimit", 10,
                "categoryIds", List.of(categoryId),
                "itemImages", List.of(Map.of("tempFileId", tempFileId, "imgType", "REPRESENTATIVE")),
                "itemOptions", List.of(Map.of("description", "기본", "additionalPrice", 0, "stock", 10))));
        assertThat(created.getStatusCode()).as(created.getBody()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(created.getBody());
    }

    private ResponseEntity<String> uploadTempImage(String accessToken) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.IMAGE_PNG);
        partHeaders.setContentDisposition(ContentDisposition.formData().name("images").filename("a.png").build());
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("images", new HttpEntity<>(PNG, partHeaders));
        return restTemplate.exchange("/api/files/temp", HttpMethod.POST,
                new HttpEntity<>(body, multipartHeaders(accessToken)), String.class);
    }

    private ResponseEntity<String> multipart(HttpMethod method, String path, String accessToken, Object itemRequest) throws Exception {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_JSON);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("itemRequest", new HttpEntity<>(objectMapper.writeValueAsString(itemRequest), partHeaders));
        return restTemplate.exchange(path, method, new HttpEntity<>(body, multipartHeaders(accessToken)), String.class);
    }

    private HttpHeaders multipartHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private String loginAsAdmin() {
        String email = signUp();
        jdbcTemplate.update("UPDATE member SET role = 'ADMIN' WHERE email = ?", email);
        return login(email);
    }

    private String signUpAndLogin() {
        return login(signUp());
    }

    private String signUp() {
        String unique = unique();
        String email = unique + "@example.com";
        ResponseEntity<String> response = send(HttpMethod.POST, "/api/members/signup", null, Map.of(
                "email", email, "memberName", "회원" + unique, "memberPwd", PASSWORD, "tel", uniqueTel()));
        assertThat(response.getStatusCode().is2xxSuccessful()).as(response.getBody()).isTrue();
        return email;
    }

    private String login(String email) {
        ResponseEntity<String> response = send(HttpMethod.POST, "/api/auth/login", null,
                Map.of("email", email, "memberPwd", PASSWORD));
        assertThat(response.getStatusCode()).as(response.getBody()).isEqualTo(HttpStatus.OK);
        return response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION).substring("Bearer ".length());
    }

    private JsonNode ok(HttpMethod method, String path, String accessToken, Object body) throws Exception {
        ResponseEntity<String> response = send(method, path, accessToken, body);
        assertThat(response.getStatusCode()).as(path + " " + response.getBody()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String accessToken, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private String uniqueTel() {
        return String.format("010%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
