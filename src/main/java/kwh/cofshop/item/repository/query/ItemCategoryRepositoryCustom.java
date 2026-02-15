package kwh.cofshop.item.repository.query;

import java.util.List;

public interface ItemCategoryRepositoryCustom {

    void deleteByItemIdAndCategoryIds(Long itemId, List<Long> categoryIds);

}
