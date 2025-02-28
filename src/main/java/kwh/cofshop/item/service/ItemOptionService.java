package kwh.cofshop.item.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.ItemOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class ItemOptionService {

    private final ItemOptionRepository itemOptionRepository;
    private final EntityManager entityManager;

    @Transactional
    public List<ItemOption> saveItemOptions(Item item, List<ItemOptionRequestDto> optionRequestDto) {
        List<ItemOption> itemOptions = optionRequestDto.stream()
                .map(dto -> ItemOption.createOption(
                        dto.getDescription(),
                        dto.getAdditionalPrice(),
                        dto.getOptionNo(),
                        dto.getStock(),
                        item
                ))
                .toList();
        return itemOptionRepository.saveAll(itemOptions);
    }

    @Transactional
    public void deleteItemOptions(Long itemId, List<Long> deleteOptionIds) {
        if (deleteOptionIds != null && !deleteOptionIds.isEmpty()) {
            itemOptionRepository.deleteByItemIdAndId(itemId, deleteOptionIds);
        }
    }

    @Transactional
    public void updateExistingItemOptions(Item item, List<ItemOptionRequestDto> existingItemOptions) {
        if (existingItemOptions != null && !existingItemOptions.isEmpty()) {
            List<ItemOption> existingOptions = itemOptionRepository.findByItemId(item.getId());
            Map<Long, ItemOption> existingOptionMap = existingOptions.stream()
                    .collect(Collectors.toMap(ItemOption::getId, option -> option));

            for (ItemOptionRequestDto optionDto : existingItemOptions) {
                if (optionDto.getId() != null && existingOptionMap.containsKey(optionDto.getId())) {
                    ItemOption existingOption = existingOptionMap.get(optionDto.getId());
                    existingOption.updateOption(optionDto);
                }
            }
        }
    }

    @Transactional
    public void addNewItemOptions(Item item, List<ItemOptionRequestDto> addItemOptions) {
        if (addItemOptions != null && !addItemOptions.isEmpty()) {
            List<ItemOption> newOptions = addItemOptions.stream()
                    .filter(optionDto -> optionDto.getId() == null) // ID 없는 경우만 추가
                    .map(dtos -> ItemOption.createOption(
                            dtos.getDescription(),
                            dtos.getAdditionalPrice(),
                            dtos.getOptionNo(),
                            dtos.getStock(),
                            item
                    ))
                    .toList();

            if (!newOptions.isEmpty()) {
                itemOptionRepository.saveAll(newOptions);
            }
        }
    }

    public void updateItemOptions(Item item, ItemUpdateRequestDto dto) {
        updateExistingItemOptions(item, dto.getExistingItemOptions());
        addNewItemOptions(item, dto.getAddItemOptions()); // 등록
        deleteItemOptions(item.getId(), dto.getDeleteOptionIds()); // 후 삭제해야 함
    }

}
