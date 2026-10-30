package kwh.cofshop.order.policy;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BasicDeliveryPolicyTest {

    private final BasicDeliveryPolicy policy = new BasicDeliveryPolicy();

    @Test
    @DisplayName("배송비 계산: null 배송비는 0으로 처리")
    void calculate_nullDeliveryFeeAsZero() {
        Item freeDeliveryItem = createItem("원두A", null);
        Item paidDeliveryItem = createItem("원두B", 3000);

        ItemOption option1 = ItemOption.builder().item(freeDeliveryItem).stock(10).build();
        ItemOption option2 = ItemOption.builder().item(paidDeliveryItem).stock(10).build();
        ItemOption option3 = ItemOption.builder().item(paidDeliveryItem).stock(10).build();

        int deliveryFee = policy.calculate(List.of(option1, option2, option3));

        assertThat(deliveryFee).isEqualTo(3000);
    }

    private Item createItem(String name, Integer deliveryFee) {
        return Item.builder()
                .itemName(name)
                .price(1000)
                .discount(0)
                .deliveryFee(deliveryFee)
                .origin("KOR")
                .itemLimit(10)
                .seller(Member.builder()
                        .id(1L)
                        .email("seller@example.com")
                        .memberName("seller")
                        .memberPwd("pw")
                        .tel("01011112222")
                        .build())
                .build();
    }
}
