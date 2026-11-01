package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.ItemOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ItemOptionService {

    private final ItemOptionRepository itemOptionRepository;

    @Transactional
    public List<ItemOption> saveItemOptions(Item item, List<ItemOptionRequestDto> optionRequestDto) {
        if (optionRequestDto == null || optionRequestDto.isEmpty()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        List<ItemOption> itemOptions = optionRequestDto.stream()
                .map(dto -> ItemOption.createOption(
                        dto.getDescription(),
                        dto.getAdditionalPrice(),
                        dto.getStock(),
                        item
                ))
                .toList();
        return itemOptionRepository.saveAll(itemOptions);
    }

    public void deleteItemOptions(Long itemId, List<Long> deleteOptionIds) {
        if (deleteOptionIds == null || deleteOptionIds.isEmpty()) {
            return;
        }
        itemOptionRepository.deleteByItemIdAndItemOptionId(itemId, deleteOptionIds);
    }

    public void updateExistingItemOptions(Item item, List<ItemOptionRequestDto> existingItemOptions) {
        if (existingItemOptions == null || existingItemOptions.isEmpty()) {
            return;
        }

        List<Long> requestedOptionIds = existingItemOptions.stream()
                .map(ItemOptionRequestDto::getId)
                .toList();
        if (requestedOptionIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        Set<Long> uniqueRequestedOptionIds = new HashSet<>(requestedOptionIds);
        if (uniqueRequestedOptionIds.size() != requestedOptionIds.size()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        Map<Long, ItemOption> existingOptionMap = itemOptionRepository.findByItemId(item.getId()).stream()
                .collect(Collectors.toMap(ItemOption::getId, option -> option));
        if (!existingOptionMap.keySet().containsAll(uniqueRequestedOptionIds)) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        for (ItemOptionRequestDto optionDto : existingItemOptions) {
            ItemOption existingOption = existingOptionMap.get(optionDto.getId());
            existingOption.updateOption(
                    optionDto.getDescription(),
                    optionDto.getAdditionalPrice(),
                    resolveStock(existingOption, optionDto)
            );
        }
    }

    // 재고는 절댓값으로 저장하므로, 읽은 뒤 들어온 주문 차감을 덮어쓰지 않도록 확인
    private Integer resolveStock(ItemOption existingOption, ItemOptionRequestDto optionDto) {
        Integer expectedStock = optionDto.getExpectedStock();
        if (expectedStock == null) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        // 재고를 바꾸지 않는 수정은 그 사이 재고가 변했어도 현재 값을 유지
        if (expectedStock.equals(optionDto.getStock())) {
            return existingOption.getStock();
        }
        if (!expectedStock.equals(existingOption.getStock())) {
            throw new BusinessException(BusinessErrorCode.ITEM_STOCK_CHANGED);
        }
        return optionDto.getStock();
    }

    public void addNewItemOptions(Item item, List<ItemOptionRequestDto> addItemOptions) {
        if (addItemOptions == null || addItemOptions.isEmpty()) {
            return;
        }

        List<ItemOption> newOptions = addItemOptions.stream()
                .filter(optionDto -> optionDto.getId() == null)
                .map(dto -> ItemOption.createOption(
                        dto.getDescription(),
                        dto.getAdditionalPrice(),
                        dto.getStock(),
                        item
                ))
                .toList();

        if (newOptions.isEmpty()) {
            return;
        }
        itemOptionRepository.saveAll(newOptions);
    }

    @Transactional
    public void updateItemOptions(Item item, ItemUpdateRequestDto dto) {
        deleteItemOptions(item.getId(), dto.getDeleteOptionIds());
        updateExistingItemOptions(item, dto.getExistingItemOptions());
        addNewItemOptions(item, dto.getAddItemOptions());
    }
}
