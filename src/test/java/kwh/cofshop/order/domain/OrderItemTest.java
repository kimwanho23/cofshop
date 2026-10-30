package kwh.cofshop.order.domain;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.domain.OptionState;
import kwh.cofshop.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    @Test
    @DisplayName("주문 아이템 생성: 수량은 1 이상이어야 함")
    void createOrderItem_invalidQuantity() {
        ItemOption option = createOption(100);

        assertThatThrownBy(() -> OrderItem.createOrderItem(option, 0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("총 금액 계산: int 범위를 넘어도 long으로 보존")
    void getTotalPrice_overflowSafe() {
        OrderItem orderItem = OrderItem.builder()
                .orderPrice(2_000_000_000)
                .discountRate(0)
                .quantity(2)
                .item(createItem())
                .itemOption(createOption(10))
                .build();

        assertThat(orderItem.getTotalPrice()).isEqualTo(4_000_000_000L);
    }

    @Test
    @DisplayName("주문 아이템 생성: 옵션 할인율을 금액에 반영")
    void createOrderItem_appliesOptionDiscountRate() {
        ItemOption option = ItemOption.builder()
                .item(createItem())
                .additionalPrice(0)
                .discountRate(10)
                .stock(10)
                .optionState(OptionState.SELL)
                .build();

        OrderItem orderItem = OrderItem.createOrderItem(option, 2);

        assertThat(orderItem.getDiscountRate()).isEqualTo(10);
        assertThat(orderItem.getDiscountedPrice()).isEqualTo(900);
        assertThat(orderItem.getTotalPrice()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("빌더: 식별자와 할인율을 보존")
    void builder_keepsIdAndDiscountRate() {
        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .orderPrice(1000)
                .discountRate(10)
                .quantity(1)
                .build();

        assertThat(orderItem.getId()).isEqualTo(1L);
        assertThat(orderItem.getDiscountRate()).isEqualTo(10);
    }

    private Item createItem() {
        return Item.builder()
                .itemName("커피")
                .price(1000)
                .deliveryFee(0)
                .origin("브라질")
                .itemLimit(10)
                .seller(Member.builder()
                        .id(1L)
                        .email("seller@example.com")
                        .memberName("판매자")
                        .memberPwd("pw")
                        .tel("01012341234")
                        .build())
                .build();
    }

    private ItemOption createOption(int stock) {
        return ItemOption.builder()
                .item(createItem())
                .additionalPrice(0)
                .stock(stock)
                .optionState(OptionState.SELL)
                .build();
    }
}
