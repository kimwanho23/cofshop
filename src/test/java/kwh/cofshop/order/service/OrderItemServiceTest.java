package kwh.cofshop.order.service;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.domain.ItemState;
import kwh.cofshop.item.domain.OptionState;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderItemRepository;
import kwh.cofshop.order.repository.projection.ItemPurchasedQuantityProjection;
import kwh.cofshop.item.api.ItemReadPort;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
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

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    @Test
    @DisplayName("누적 구매 제한: 이전 구매량과 합쳐 제한을 넘으면 실패")
    void validateMemberPurchaseLimit_exceeded() {
        ItemOption option = createOption(10L, createItem(1L), 100, 10);
        when(orderItemRepository.sumPurchasedQuantities(1L, java.util.Set.of(1L), OrderState.CANCELLED))
                .thenReturn(List.of(purchased(1L, 9L)));

        assertThatThrownBy(() -> orderItemService.validateMemberPurchaseLimit(1L, List.of(request(2)), List.of(option)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ITEM_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("누적 구매 제한: 제한까지는 허용")
    void validateMemberPurchaseLimit_withinLimit() {
        ItemOption option = createOption(10L, createItem(1L), 100, 10);
        when(orderItemRepository.sumPurchasedQuantities(1L, java.util.Set.of(1L), OrderState.CANCELLED))
                .thenReturn(List.of(purchased(1L, 8L)));

        orderItemService.validateMemberPurchaseLimit(1L, List.of(request(2)), List.of(option));
    }

    @Test
    @DisplayName("주문 아이템 생성: 판매 중지 상품은 실패")
    void createOrderItems_stoppedItem() {
        Item item = createItem(1L);
        item.changeItemState(ItemState.STOP);
        ItemOption option = createOption(10L, item, 100, 10);

        assertThatThrownBy(() -> orderItemService.createOrderItems(List.of(request(1)), List.of(option)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ITEM_NOT_ON_SALE);
    }

    private OrderItemRequestDto request(int quantity) {
        OrderItemRequestDto dto = new OrderItemRequestDto();
        dto.setItemId(1L);
        dto.setOptionId(10L);
        dto.setQuantity(quantity);
        return dto;
    }

    private ItemPurchasedQuantityProjection purchased(Long itemId, Long quantity) {
        return new ItemPurchasedQuantityProjection() {
            @Override
            public Long getItemId() {
                return itemId;
            }

            @Override
            public Long getQuantity() {
                return quantity;
            }
        };
    }

    @Test
    @DisplayName("옵션 조회")
    void getItemOptionsWithLock() {
        OrderItemRequestDto dto = new OrderItemRequestDto();
        dto.setItemId(1L);
        dto.setOptionId(10L);
        dto.setQuantity(1);

        ItemOption option = createOption(10L, createItem(1L), 100, 10);

        when(itemReadPort.findItemOptionsByIdsWithLock(List.of(10L))).thenReturn(List.of(option));

        List<ItemOption> result = orderItemService.getItemOptionsWithLock(List.of(dto));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("재고 복구: 락으로 다시 읽은 옵션에 같은 옵션 수량을 합쳐 반영")
    void restoreStock_usesLockedOptions() {
        ItemOption staleOption = createOption(10L, createItem(1L), 100, 3);
        ItemOption lockedOption = createOption(10L, createItem(1L), 100, 7);
        OrderItem first = OrderItem.builder().itemOption(staleOption).quantity(1).build();
        OrderItem second = OrderItem.builder().itemOption(staleOption).quantity(2).build();
        when(itemReadPort.findItemOptionsByIdsWithLock(List.of(10L))).thenReturn(List.of(lockedOption));

        orderItemService.restoreStock(List.of(first, second));

        assertThat(lockedOption.getStock()).isEqualTo(10);
        assertThat(staleOption.getStock()).isEqualTo(3);
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

    @Test
    @DisplayName("주문 아이템 생성: 수량은 1 이상이어야 함")
    void createOrderItems_invalidQuantity() {
        OrderItemRequestDto dto = new OrderItemRequestDto();
        dto.setItemId(1L);
        dto.setOptionId(10L);
        dto.setQuantity(0);

        ItemOption option = createOption(10L, createItem(1L), 100, 100);

        assertThatThrownBy(() -> orderItemService.createOrderItems(List.of(dto), List.of(option)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("주문 아이템 생성: 같은 상품 분할 수량 합계 검증은 int 오버플로우 없이 동작")
    void createOrderItems_splitLinesQuantityOverflowSafe() {
        OrderItemRequestDto first = new OrderItemRequestDto();
        first.setItemId(1L);
        first.setOptionId(10L);
        first.setQuantity(Integer.MAX_VALUE);

        OrderItemRequestDto second = new OrderItemRequestDto();
        second.setItemId(1L);
        second.setOptionId(11L);
        second.setQuantity(1);

        Item item = createItem(1L);
        ReflectionTestUtils.setField(item, "itemLimit", Integer.MAX_VALUE);
        ItemOption firstOption = createOption(10L, item, 100, Integer.MAX_VALUE);
        ItemOption secondOption = createOption(11L, item, 100, 1);

        assertThatThrownBy(() -> orderItemService.createOrderItems(
                List.of(first, second),
                List.of(firstOption, secondOption)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ITEM_LIMIT_EXCEEDED);
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
