package kwh.cofshop.item.repository.custom;

import kwh.cofshop.item.domain.ItemCategory;

import java.util.List;

public interface ItemCategoryRepositoryCustom {

    void deleteByItemIdAndCategoryIds(Long itemId, List<Long> categoryIds);

    List<ItemCategory> findByItemIdWithLock(Long id);
}
