package kwh.cofshop.order.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemService {

    private final ItemOptionRepository itemOptionRepository;

    // 옵션 조회
    public List<ItemOption> getItemOptionsWithLock(List<OrderItemRequestDto> itemDtoList) {
        List<Long> optionIds = itemDtoList.stream()
                .map(OrderItemRequestDto::getOptionId)
                .toList();

        return itemOptionRepository.findAllByIdInWithLock(optionIds);
    }

    // 주문 생성
    public List<OrderItem> createOrderItems(List<OrderItemRequestDto> dtoList, List<ItemOption> options) {
        Map<Long, ItemOption> optionMap = options.stream()
                .collect(Collectors.toMap(ItemOption::getId, Function.identity()));
        Map<Long, Integer> requestedQuantityByItemId = dtoList.stream()
                .collect(Collectors.groupingBy(
                        OrderItemRequestDto::getItemId,
                        Collectors.summingInt(OrderItemRequestDto::getQuantity)
                ));

        List<OrderItem> result = new ArrayList<>();
        for (OrderItemRequestDto dto : dtoList) {
            ItemOption option = optionMap.get(dto.getOptionId());
            if (option == null || !option.getItem().getId().equals(dto.getItemId())) {
                throw new BusinessException(BusinessErrorCode.ITEM_OPTION_NOT_FOUND);
            }
            option.validatePurchasable();
            Integer itemLimit = option.getItem().getItemLimit();
            int requestedQuantity = requestedQuantityByItemId.getOrDefault(dto.getItemId(), dto.getQuantity());
            if (itemLimit != null && requestedQuantity > itemLimit) {
                throw new BusinessException(BusinessErrorCode.ITEM_LIMIT_EXCEEDED);
            }
            result.add(OrderItem.createOrderItem(option, dto.getQuantity()));
        }
        return result;
    }
}
