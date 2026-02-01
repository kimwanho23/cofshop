package kwh.cofshop.order.service;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private ItemOptionRepository itemOptionRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    @Test
    @DisplayName("옵션 조회")
    void getItemOptionsWithLock() {
        OrderItemRequestDto dto = new OrderItemRequestDto();
        dto.setOptionId(10L);

        ItemOption option = createOption(createItem(), 100, 10);

        when(itemOptionRepository.findAllByIdInWithLock(List.of(10L))).thenReturn(List.of(option));

        List<ItemOption> result = orderItemService.getItemOptionsWithLock(List.of(dto));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("주문 아이템 생성")
    void createOrderItems() {
        OrderItemRequestDto dto = new OrderItemRequestDto();
        dto.setQuantity(2);

        ItemOption option = createOption(createItem(), 100, 10);

        List<OrderItem> result = orderItemService.createOrderItems(List.of(dto), List.of(option));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuantity()).isEqualTo(2);
    }

    private Item createItem() {
        return Item.builder()
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
    }

    private ItemOption createOption(Item item, int additionalPrice, int stock) {
        return ItemOption.builder()
                .item(item)
                .additionalPrice(additionalPrice)
                .stock(stock)
                .build();
    }
}