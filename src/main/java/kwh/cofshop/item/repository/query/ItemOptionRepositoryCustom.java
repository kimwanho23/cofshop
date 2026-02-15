package kwh.cofshop.item.repository.query;

import kwh.cofshop.item.domain.ItemOption;

import java.util.List;

public interface ItemOptionRepositoryCustom {

    void deleteByItemIdAndItemOptionId(Long itemId, List<Long> ids);

}
