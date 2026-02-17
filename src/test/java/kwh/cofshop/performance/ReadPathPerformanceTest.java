package kwh.cofshop.performance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import kwh.cofshop.coupon.messaging.CouponIssueEventPublisher;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.domain.ImgType;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemCategory;
import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.repository.CategoryRepository;
import kwh.cofshop.item.repository.ItemCategoryRepository;
import kwh.cofshop.item.repository.ItemImgRepository;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.item.service.ItemService;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.order.service.OrderService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_PERF", matches = "true")
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class ReadPathPerformanceTest {

    @MockitoBean
    private CouponIssueEventPublisher couponIssueEventPublisher;

    @Autowired
    private ItemService itemService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemCategoryRepository itemCategoryRepository;
    @Autowired
    private ItemOptionRepository itemOptionRepository;
    @Autowired
    private ItemImgRepository itemImgRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Statistics statistics;
    private Long sampleMemberId;
    private Long sampleItemId;
    private Long sampleOrderId;

    @BeforeEach
    void setUp() {
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.statistics.setStatisticsEnabled(true);
        seedData();
    }

    @Test
    void measureReadPaths() throws Exception {
        Measurement itemDetail = measure("/api/item/{itemId}", 15, () -> {
            ItemResponseDto responseDto = itemService.getItem(sampleItemId);
            assertThat(responseDto.getId()).isEqualTo(sampleItemId);
        });

        Measurement popularItems = measure("/api/item/populars", 15, () -> {
            List<ItemResponseDto> responses = itemService.getPopularItem(10);
            assertThat(responses).isNotEmpty();
        });

        Measurement orderSummary = measure("/api/orders/{orderId}", 15, () -> {
            OrderResponseDto responseDto = orderService.orderSummary(sampleMemberId, sampleOrderId);
            assertThat(responseDto.getOrderId()).isEqualTo(sampleOrderId);
        });

        Measurement memberOrders = measure("/api/orders/me", 15, () ->
                assertThat(orderService.memberOrders(sampleMemberId, PageRequest.of(0, 20))).isNotNull()
        );

        Measurement allOrders = measure("/api/orders", 15, () ->
                assertThat(orderService.allOrderList(PageRequest.of(0, 20))).isNotNull()
        );

        assertThat(itemDetail.avgQueryCount()).isLessThanOrEqualTo(4.5);
        assertThat(popularItems.avgQueryCount()).isLessThanOrEqualTo(5.5);
        assertThat(orderSummary.avgQueryCount()).isLessThanOrEqualTo(3.5);
        assertThat(memberOrders.avgQueryCount()).isLessThanOrEqualTo(4.5);
        assertThat(allOrders.avgQueryCount()).isLessThanOrEqualTo(4.5);

        String report = buildReport(List.of(
                itemDetail,
                popularItems,
                orderSummary,
                memberOrders,
                allOrders
        ));

        Path reportPath = Path.of("build/reports/performance/read-path-performance.txt");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(
                reportPath,
                report,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        System.out.println(report);
    }

    private Measurement measure(String path, int iterations, Runnable action) {
        for (int i = 0; i < 3; i++) {
            entityManager.clear();
            action.run();
        }

        statistics.clear();
        long startedAt = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            entityManager.clear();
            action.run();
        }

        long elapsedNanos = System.nanoTime() - startedAt;
        double avgMs = elapsedNanos / 1_000_000.0 / iterations;
        double avgQueryCount = statistics.getPrepareStatementCount() / (double) iterations;
        return new Measurement(path, avgQueryCount, avgMs);
    }

    private String buildReport(List<Measurement> measurements) {
        StringBuilder builder = new StringBuilder();
        builder.append("=== Read Path Performance Report ===\n");
        builder.append("MeasuredAt=").append(LocalDateTime.now()).append('\n');
        builder.append("Dataset=items:40, orders:120, pageSize:20\n\n");

        for (Measurement measurement : measurements) {
            builder.append(measurement.path())
                    .append(" | avgQueryCount=")
                    .append(String.format("%.2f", measurement.avgQueryCount()))
                    .append(" | avgResponseMs=")
                    .append(String.format("%.2f", measurement.avgResponseMs()))
                    .append('\n');
        }

        builder.append("\n=== EXPLAIN (H2) ===\n");
        builder.append("popular-item-ids: ")
                .append(explain("SELECT oi.item_id FROM order_items oi JOIN orders o ON oi.order_id = o.order_id " +
                        "WHERE o.order_state IN ('PAID','PREPARING_FOR_SHIPMENT','SHIPPING','DELIVERED','COMPLETED') " +
                        "GROUP BY oi.item_id ORDER BY SUM(oi.quantity) DESC LIMIT 10"))
                .append('\n');
        builder.append("item-base-by-ids: ")
                .append(explain("SELECT i.item_id, i.item_name, i.price FROM item i WHERE i.item_id IN (" + sampleItemId + ")"))
                .append('\n');
        builder.append("order-header-page: ")
                .append(explain("SELECT o.order_id, o.order_state FROM orders o WHERE o.member_id = " + sampleMemberId +
                        " ORDER BY o.create_date DESC, o.order_id DESC LIMIT 20 OFFSET 0"))
                .append('\n');
        builder.append("order-items-by-order-ids: ")
                .append(explain("SELECT oi.order_id, oi.item_id, oi.option_id, oi.quantity FROM order_items oi " +
                        "WHERE oi.order_id IN (" + sampleOrderId + ")"))
                .append('\n');

        return builder.toString();
    }

    private String explain(String sql) {
        try {
            return String.valueOf(jdbcTemplate.queryForObject("EXPLAIN " + sql, Object.class));
        } catch (Exception e) {
            return "EXPLAIN_FAILED(" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")";
        }
    }

    private void seedData() {
        if (sampleOrderId != null) {
            return;
        }

        Member seller = memberRepository.save(Member.builder()
                .email("perf-seller+" + UUID.randomUUID() + "@example.com")
                .memberName("perf-seller")
                .memberPwd("pw")
                .tel("01011112222")
                .build());

        Member member = memberRepository.save(Member.builder()
                .email("perf-member+" + UUID.randomUUID() + "@example.com")
                .memberName("perf-member")
                .memberPwd("pw")
                .tel("01033334444")
                .build());

        sampleMemberId = member.getId();

        Category category = categoryRepository.save(Category.builder()
                .name("perf-category-" + UUID.randomUUID())
                .build());

        List<ItemOption> options = new ArrayList<>();

        for (int i = 0; i < 40; i++) {
            Item item = itemRepository.save(Item.builder()
                    .itemName("perf-item-" + i)
                    .price(3000 + i)
                    .discount(5)
                    .deliveryFee(0)
                    .origin("KOR")
                    .itemLimit(1000)
                    .seller(seller)
                    .build());

            if (i == 0) {
                sampleItemId = item.getId();
            }

            itemCategoryRepository.save(new ItemCategory(item, category));

            ItemOption option = itemOptionRepository.save(ItemOption.createOption(
                    "opt-" + i,
                    100,
                    100_000,
                    item
            ));
            options.add(option);

            itemImgRepository.save(ItemImg.createImg(
                    "img-" + i + ".jpg",
                    "ori-" + i + ".jpg",
                    "/images/" + i + ".jpg",
                    ImgType.REPRESENTATIVE,
                    item
            ));
        }

        Address address = Address.builder()
                .city("Seoul")
                .street("Teheran-ro")
                .zipCode("12345")
                .build();

        for (int i = 0; i < 120; i++) {
            ItemOption option = options.get(i % options.size());
            int quantity = 1 + (i % 3);

            OrderItem orderItem = OrderItem.createOrderItem(option, quantity);
            Order order = Order.createOrder(member, address, "perf-test", List.of(orderItem));
            order.changeOrderState(OrderState.PAID);
            order.finalizePrice(order.getTotalPrice(), 0, 0);

            Order savedOrder = orderRepository.save(order);
            if (i == 0) {
                sampleOrderId = savedOrder.getId();
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    private record Measurement(String path, double avgQueryCount, double avgResponseMs) {
    }
}
