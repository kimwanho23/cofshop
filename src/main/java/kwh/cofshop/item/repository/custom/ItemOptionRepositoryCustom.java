package kwh.cofshop.item.repository.custom;

import kwh.cofshop.item.domain.ItemOption;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemOptionRepositoryCustom {

    Optional<ItemOption> findByIdWithLock(Long optionId);

    void deleteByItemIdAndId(Long itemId, List<Long> ids);
}
