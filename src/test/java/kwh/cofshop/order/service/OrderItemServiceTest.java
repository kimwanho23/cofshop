package kwh.cofshop.order.service;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.domain.OptionState;
import kwh.cofshop.item.api.ItemReadPort;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private ItemReadPort itemReadPort;

    @InjectMocks
    private OrderItemService orderItemService;

    @Test
    @DisplayName("옵션 조회")
    void getItemOptionsWithLock() {
        OrderItemRequestDto dto = new OrderItemRequestDto();
        dto.setOptionId(10L);

        ItemOption option = createOption(10L, createItem(1L), 100, 10);

        when(itemReadPort.findItemOptionsByIdsWithLock(List.of(10L))).thenReturn(List.of(option));

        List<ItemOption> result = orderItemService.getItemOptionsWithLock(List.of(dto));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("주문 아이템 생성")
    void createOrderItems() {
        OrderItemRequestDto dto = new OrderItemRequestDto();
        dto.setItemId(1L);
        dto.setOptionId(10L);
        dto.setQuantity(2);

        ItemOption option = createOption(10L, createItem(1L), 100, 10);

        List<OrderItem> result = orderItemService.createOrderItems(List.of(dto), List.of(option));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("주문 아이템 생성 시 상품 할인율 반영")
    void createOrderItems_applyItemDiscount() {
        OrderItemRequestDto dto = new OrderItemRequestDto();
        dto.setItemId(1L);
        dto.setOptionId(10L);
        dto.setQuantity(1);

        Item discountedItem = createItem(1L);
        ReflectionTestUtils.setField(discountedItem, "discount", 10);
        ItemOption option = createOption(10L, discountedItem, 100, 10);

        List<OrderItem> result = orderItemService.createOrderItems(List.of(dto), List.of(option));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderPrice()).isEqualTo(1000);
        assertThat(result.get(0).getTotalPrice()).isEqualTo(1000);
    }

    @Test
    @DisplayName("주문 아이템 생성: 상품 구매 제한 수량 초과")
    void createOrderItems_exceedItemLimit() {
        OrderItemRequestDto dto = new OrderItemRequestDto();
        dto.setItemId(1L);
        dto.setOptionId(10L);
        dto.setQuantity(11);

        ItemOption option = createOption(10L, createItem(1L), 100, 100);

        assertThatThrownBy(() -> orderItemService.createOrderItems(List.of(dto), List.of(option)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("주문 아이템 생성: 같은 상품 분할 요청도 구매 제한 수량 초과로 처리")
    void createOrderItems_exceedItemLimitBySplitLines() {
        OrderItemRequestDto first = new OrderItemRequestDto();
        first.setItemId(1L);
        first.setOptionId(10L);
        first.setQuantity(6);

        OrderItemRequestDto second = new OrderItemRequestDto();
        second.setItemId(1L);
        second.setOptionId(11L);
        second.setQuantity(6);

        Item item = createItem(1L);
        ItemOption firstOption = createOption(10L, item, 100, 100);
        ItemOption secondOption = createOption(11L, item, 200, 100);

        assertThatThrownBy(() -> orderItemService.createOrderItems(
                List.of(first, second),
                List.of(firstOption, secondOption)
        )).isInstanceOf(BusinessException.class);
    }

    private Item createItem(Long id) {
        Item item = Item.builder()
                .itemName("커피")
                .price(1000)
                .deliveryFee(0)
                .origin("브라질")
                .itemLimit(10)
                .seller(Member.builder()
                        .id(2L)
                        .email("seller@example.com")
                        .memberName("판매자")
                        .memberPwd("pw")
                        .tel("01099998888")
                        .build())
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private ItemOption createOption(Long id, Item item, int additionalPrice, int stock) {
        ItemOption option = ItemOption.builder()
                .item(item)
                .additionalPrice(additionalPrice)
                .stock(stock)
                .optionState(OptionState.SELL)
                .build();
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }
}
