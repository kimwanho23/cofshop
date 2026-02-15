package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.ItemOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
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


/*    @Transactional
    public void updateItemOptionTest(Item item, ItemUpdateRequestDto dto) {
        // 1) 기존 옵션 조회
        List<ItemOption> existingOptions = itemOptionRepository.findByItemId(item.getId());
        Map<Long, ItemOption> existingOptionMap = existingOptions.stream()
                .collect(Collectors.toMap(ItemOption::getId, option -> option));

        // 2) 삭제할 옵션 처리
        List<Long> deleteOptionIds = dto.getDeleteOptionIds();
        if (deleteOptionIds != null && !deleteOptionIds.isEmpty()) {
            itemOptionRepository.deleteByItemIdAndId(item.getId(), deleteOptionIds);
        }
        entityManager.flush();
        entityManager.clear();

        // 3) 옵션 수정 (ID가 존재하는 경우)
        List<ItemOptionRequestDto> existingItemOptions = dto.getExistingItemOptions();
        if (existingItemOptions != null && !existingItemOptions.isEmpty()) {
            for (ItemOptionRequestDto optionDto : existingItemOptions) {
                if (optionDto.getId() != null && existingOptionMap.containsKey(optionDto.getId())) {
                    ItemOption existingOption = itemOptionRepository.findById(optionDto.getId())
                            .orElseThrow(() -> new EntityNotFoundException("ItemOption not found: " + optionDto.getId()));
                    existingOption.updateOption(optionDto);
                }
            }
        }

        // 4) 새로운 옵션 추가 (ID 없는 경우)
        List<ItemOptionRequestDto> addItemOptions = dto.getAddItemOptions();
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
    }*/


    // 옵션 삭제
    public void deleteItemOptions(Long itemId, List<Long> deleteOptionIds) {
        if (deleteOptionIds != null && !deleteOptionIds.isEmpty()) {
            itemOptionRepository.deleteByItemIdAndItemOptionId(itemId, deleteOptionIds);
        }
    }

    // 기존에 존재하는 옵션의 변경 감지
    public void updateExistingItemOptions(Item item, List<ItemOptionRequestDto> existingItemOptions) {
        if (existingItemOptions != null && !existingItemOptions.isEmpty()) {
            List<ItemOption> existingOptions = itemOptionRepository.findByItemId(item.getId());
            Map<Long, ItemOption> existingOptionMap = existingOptions.stream()
                    .collect(Collectors.toMap(ItemOption::getId, option -> option));

            for (ItemOptionRequestDto optionDto : existingItemOptions) {
                if (optionDto.getId() != null && existingOptionMap.containsKey(optionDto.getId())) {
                    ItemOption existingOption = existingOptionMap.get(optionDto.getId());
                    existingOption.updateOption(
                            optionDto.getDescription(),
                            optionDto.getAdditionalPrice(),
                            optionDto.getStock()
                    );
                }
            }
        }
    }

    // 새로운 옵션 등록
    public void addNewItemOptions(Item item, List<ItemOptionRequestDto> addItemOptions) {
        if (addItemOptions != null && !addItemOptions.isEmpty()) {
            List<ItemOption> newOptions = addItemOptions.stream()
                    .filter(optionDto -> optionDto.getId() == null) // ID 없는 경우만 추가
                    .map(dtos -> ItemOption.createOption(
                            dtos.getDescription(),
                            dtos.getAdditionalPrice(),
                            dtos.getStock(),
                            item
                    ))
                    .toList();

            if (!newOptions.isEmpty()) {
                itemOptionRepository.saveAll(newOptions);
            }
        }
    }

    @Transactional
    public void updateItemOptions(Item item, ItemUpdateRequestDto dto) {
        deleteItemOptions(item.getId(), dto.getDeleteOptionIds());
        updateExistingItemOptions(item, dto.getExistingItemOptions());
        addNewItemOptions(item, dto.getAddItemOptions());
    }

}
