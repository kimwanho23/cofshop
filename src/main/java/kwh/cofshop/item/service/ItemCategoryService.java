package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemCategory;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.CategoryRepository;
import kwh.cofshop.item.repository.ItemCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class ItemCategoryService {

    private final CategoryRepository categoryRepository;
    private final ItemCategoryRepository itemCategoryRepository;

    public void deleteItemCategories(Item item, List<Long> deleteCategoryIds) {
        if (deleteCategoryIds != null && !deleteCategoryIds.isEmpty()) {
            itemCategoryRepository.deleteByItemIdAndCategoryIds(item.getId(), deleteCategoryIds);
        }
    }

    public void addItemCategories(Item item, List<Long> addCategoryIds) {
        if (addCategoryIds != null && !addCategoryIds.isEmpty()) {
            if (item.getId() == null) {
                throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
            }
            LinkedHashSet<Long> uniqueCategoryIds = new LinkedHashSet<>(addCategoryIds);
            if (uniqueCategoryIds.size() != addCategoryIds.size()) {
                throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
            }
            Set<Long> existingCategoryIds = itemCategoryRepository.findByItemId(item.getId()).stream()
                    .map(itemCategory -> itemCategory.getCategory().getId())
                    .collect(Collectors.toSet());
            if (uniqueCategoryIds.stream().anyMatch(existingCategoryIds::contains)) {
                throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
            }

            List<ItemCategory> newCategories = uniqueCategoryIds.stream()
                    .map(id -> new ItemCategory(item, categoryRepository.findById(id)
                            .orElseThrow(() -> new BusinessException(BusinessErrorCode.CATEGORY_NOT_FOUND))))
                    .toList();
            itemCategoryRepository.saveAll(newCategories);
        }
    }

    @Transactional
    public void updateItemCategories(Item item, ItemUpdateRequestDto dto) {
        deleteItemCategories(item, dto.getDeleteCategoryIds());
        addItemCategories(item, dto.getAddCategoryIds());
    }


}
