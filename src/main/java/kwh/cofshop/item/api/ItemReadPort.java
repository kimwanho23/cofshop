package kwh.cofshop.item.api;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;

import java.util.List;
import java.util.Optional;

public interface ItemReadPort {

    Optional<Item> findItemById(Long itemId);

    Optional<ItemOption> findItemOptionById(Long optionId);

    List<ItemOption> findItemOptionsByIdsWithLock(List<Long> optionIds);
}
