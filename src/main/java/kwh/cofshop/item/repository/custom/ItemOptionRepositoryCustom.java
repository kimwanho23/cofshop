package kwh.cofshop.item.repository.custom;

import kwh.cofshop.item.domain.ItemOption;

import java.util.Optional;

public interface ItemOptionRepositoryCustom {

    Optional<ItemOption> findByIdWithLock(Long optionId);
}
