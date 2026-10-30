package kwh.cofshop.order.service;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.api.ItemReadPort;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final ItemReadPort itemReadPort;

    // 옵션 조회
    public List<ItemOption> getItemOptionsWithLock(List<OrderItemRequestDto> itemDtoList) {
        validateOrderItemDtos(itemDtoList);
        List<Long> optionIds = itemDtoList.stream()
                .map(OrderItemRequestDto::getOptionId)
                .toList();

        return itemReadPort.findItemOptionsByIdsWithLock(optionIds);
    }

    // 주문 생성
    public List<OrderItem> createOrderItems(List<OrderItemRequestDto> dtoList, List<ItemOption> options) {
        validateOrderItemDtos(dtoList);
        Map<Long, ItemOption> optionMap = options.stream()
                .collect(Collectors.toMap(ItemOption::getId, Function.identity()));
        Map<Long, Long> requestedQuantityByItemId = dtoList.stream()
                .collect(Collectors.groupingBy(
                        OrderItemRequestDto::getItemId,
                        Collectors.summingLong(OrderItemRequestDto::getQuantity)
                ));

        List<OrderItem> result = new ArrayList<>();
        for (OrderItemRequestDto dto : dtoList) {
            ItemOption option = optionMap.get(dto.getOptionId());
            if (option == null || !option.getItem().getId().equals(dto.getItemId())) {
                throw new BusinessException(BusinessErrorCode.ITEM_OPTION_NOT_FOUND);
            }
            option.validatePurchasable();
            Integer itemLimit = option.getItem().getItemLimit();
            long requestedQuantity = requestedQuantityByItemId.getOrDefault(dto.getItemId(), (long) dto.getQuantity());
            if (itemLimit != null && requestedQuantity > itemLimit) {
                throw new BusinessException(BusinessErrorCode.ITEM_LIMIT_EXCEEDED);
            }
            result.add(OrderItem.createOrderItem(option, dto.getQuantity()));
        }
        return result;
    }

    // 재고 복구: 락 없이 읽은 값을 저장하면 동시 주문의 차감이 유실되므로 락을 걸고 다시 읽음
    public void restoreStock(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        Map<Long, Integer> restoredQuantityByOptionId = orderItems.stream()
                .collect(Collectors.groupingBy(
                        orderItem -> orderItem.getItemOption().getId(),
                        Collectors.summingInt(OrderItem::getQuantity)
                ));

        itemReadPort.findItemOptionsByIdsWithLock(List.copyOf(restoredQuantityByOptionId.keySet()))
                .forEach(option -> option.addStock(restoredQuantityByOptionId.get(option.getId())));
    }

    private void validateOrderItemDtos(List<OrderItemRequestDto> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        dtoList.forEach(this::validateOrderItemDto);
    }

    private void validateOrderItemDto(OrderItemRequestDto dto) {
        if (dto == null || dto.getItemId() == null || dto.getOptionId() == null) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        validateQuantity(dto.getQuantity());
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
    }
}
