package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.ItemOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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

        Map<Long, ItemOption> existingOptionMap = itemOptionRepository.findByItemId(item.getId()).stream()
                .collect(Collectors.toMap(ItemOption::getId, option -> option));

        for (ItemOptionRequestDto optionDto : existingItemOptions) {
            if (optionDto.getId() == null) {
                continue;
            }
            ItemOption existingOption = existingOptionMap.get(optionDto.getId());
            if (existingOption == null) {
                continue;
            }
            existingOption.updateOption(
                    optionDto.getDescription(),
                    optionDto.getAdditionalPrice(),
                    optionDto.getStock()
            );
        }
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
