package kwh.cofshop.item.service;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemCategory;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.CategoryRepository;
import kwh.cofshop.item.repository.ItemCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
            List<ItemCategory> newCategories = addCategoryIds.stream()
                    .map(id -> new ItemCategory(item, categoryRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id))))
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
