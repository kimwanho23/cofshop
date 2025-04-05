package kwh.cofshop.item.repository.custom;

import jakarta.persistence.LockModeType;
import kwh.cofshop.item.domain.ItemOption;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemOptionRepositoryCustom {

    Optional<ItemOption> findByItemOptionIdWithLock(Long optionId);

    void deleteByItemIdAndItemOptionId(Long itemId, List<Long> ids);

    List<ItemOption> findByItemIdWithLock(Long itemId);

}
