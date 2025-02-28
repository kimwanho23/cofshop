package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemCategory;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.CategoryRepository;
import kwh.cofshop.item.repository.ItemCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class ItemCategoryService {

    private final CategoryRepository categoryRepository;
    private final ItemCategoryRepository itemCategoryRepository;

    @Transactional
    public void updateItemCategories(Item item, ItemUpdateRequestDto dto) {
        if (dto.getDeleteCategoryIds() != null && !dto.getDeleteCategoryIds().isEmpty()) {
            itemCategoryRepository.deleteByItemIdAndCategoryIds(item.getId(), dto.getDeleteCategoryIds());
        }

        if (dto.getAddCategoryIds() != null && !dto.getAddCategoryIds().isEmpty()) {
            List<ItemCategory> newCategories = dto.getAddCategoryIds().stream()
                    .map(id -> new ItemCategory(item, categoryRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id))))
                    .toList();
            itemCategoryRepository.saveAll(newCategories);
        }
    }

}
