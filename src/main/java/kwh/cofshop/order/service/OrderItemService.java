package kwh.cofshop.order.service;

import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        List<OrderItem> result = new ArrayList<>();
        for (int i = 0; i < dtoList.size(); i++) {
            OrderItemRequestDto dto = dtoList.get(i);
            ItemOption option = options.get(i);
            result.add(OrderItem.createOrderItem(option, dto.getQuantity()));
        }
        return result;
    }
}
