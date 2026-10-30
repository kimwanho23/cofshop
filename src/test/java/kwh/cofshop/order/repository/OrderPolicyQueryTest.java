package kwh.cofshop.order.repository;

import kwh.cofshop.config.QuerydslConfig;
import kwh.cofshop.config.auditing.JpaAuditingConfiguration;
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
import kwh.cofshop.order.repository.projection.ItemPurchasedQuantityProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order-policy;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, JpaAuditingConfiguration.class})
class OrderPolicyQueryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemOptionRepository itemOptionRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member buyer;
    private Item item;
    private ItemOption option;

    @BeforeEach
    void setUp() {
        buyer = saveMember();
        item = itemRepository.save(Item.builder()
                .itemName("커피")
                .price(1000)
                .deliveryFee(0)
                .origin("브라질")
                .seller(saveMember())
                .build());
        option = itemOptionRepository.save(ItemOption.createOption("기본", 0, 100, item));
    }

    @Test
    @DisplayName("리뷰 자격: 구매 확정 주문이 있어야 함")
    void existsPurchase_onlyCompleted() {
        saveOrder(1, OrderState.DELIVERED);
        assertThat(orderItemRepository.existsPurchase(buyer.getId(), item.getId(), OrderState.COMPLETED)).isFalse();

        saveOrder(1, OrderState.COMPLETED);
        assertThat(orderItemRepository.existsPurchase(buyer.getId(), item.getId(), OrderState.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("누적 구매량: 취소 주문을 제외하고 합산")
    void sumPurchasedQuantities_excludesCancelled() {
        saveOrder(2, OrderState.COMPLETED);
        saveOrder(3, OrderState.WAITING_FOR_PAY);
        saveOrder(4, OrderState.CANCELLED);

        List<ItemPurchasedQuantityProjection> result = orderItemRepository
                .sumPurchasedQuantities(buyer.getId(), Set.of(item.getId()), OrderState.CANCELLED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(item.getId());
        assertThat(result.get(0).getQuantity()).isEqualTo(5L);
    }

    @Test
    @DisplayName("승인 환불 재시도 대상: 승인 후 유예 시간이 지난 미완료 주문만 조회")
    void findPendingApprovedRefundOrderIds() {
        LocalDateTime now = LocalDateTime.now();
        Order stale = saveApprovedOrder(OrderState.PAID, now.minusMinutes(10));
        Order pending = saveApprovedOrder(OrderState.REFUND_PENDING, now.minusMinutes(10));
        saveApprovedOrder(OrderState.PAID, now);
        saveApprovedOrder(OrderState.CANCELLED, now.minusMinutes(10));

        List<Long> result = orderRepository.findPendingApprovedRefundOrderIds(
                OrderRefundRequestStatus.APPROVED,
                List.of(OrderState.PAID, OrderState.REFUND_PENDING),
                now.minusMinutes(5),
                PageRequest.of(0, 10)
        );

        assertThat(result).containsExactlyInAnyOrder(stale.getId(), pending.getId());
    }

    private Order saveApprovedOrder(OrderState state, LocalDateTime approvedAt) {
        Order order = newOrder(1);
        order.changeOrderState(OrderState.PAID);
        order.requestRefundRequest("환불", approvedAt);
        order.processRefundRequest(OrderRefundRequestStatus.APPROVED, "승인", approvedAt);
        order.changeOrderState(state);
        return orderRepository.saveAndFlush(order);
    }

    private void saveOrder(int quantity, OrderState state) {
        Order order = newOrder(quantity);
        order.changeOrderState(state);
        orderRepository.saveAndFlush(order);
    }

    private Order newOrder(int quantity) {
        Address address = Address.builder().city("서울").street("테헤란로").zipCode("06236").build();
        return Order.createOrder(buyer, address, null, List.of(OrderItem.createOrderItem(option, quantity)));
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
