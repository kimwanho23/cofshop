package kwh.cofshop.item.repository.custom;

import kwh.cofshop.item.domain.ItemOption;

import java.util.List;
import java.util.Optional;

public interface ItemOptionRepositoryCustom {

    Optional<ItemOption> findByItemOptionIdWithLock(Long optionId);

    void deleteByItemIdAndItemOptionId(Long itemId, List<Long> ids);

    List<ItemOption> findByItemIdWithLock(Long itemId);

}
