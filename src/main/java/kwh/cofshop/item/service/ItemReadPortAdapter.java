package kwh.cofshop.item.service;

import kwh.cofshop.item.api.ItemReadPort;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ItemReadPortAdapter implements ItemReadPort {

    private final ItemRepository itemRepository;
    private final ItemOptionRepository itemOptionRepository;

    @Override
    public Optional<Item> findItemById(Long itemId) {
        return itemRepository.findById(itemId);
    }

    @Override
    public Optional<ItemOption> findItemOptionById(Long optionId) {
        return itemOptionRepository.findById(optionId);
    }

    @Override
    public List<ItemOption> findItemOptionsByIdsWithLock(List<Long> optionIds) {
        return itemOptionRepository.findAllByIdInWithLock(optionIds);
    }
}
