package kwh.cofshop.item.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.domain.QItemOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ItemOptionRepositoryImpl implements ItemOptionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ItemOption> findByItemOptionIdWithLock(Long optionId) {
        QItemOption itemOption = QItemOption.itemOption;

        ItemOption result = queryFactory
                .selectFrom(itemOption)
                .where(itemOption.id.eq(optionId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)  // 비관적 락 설정
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public void deleteByItemIdAndItemOptionId(Long itemId, List<Long> optionIdList) {
        QItemOption itemOption = QItemOption.itemOption;
        if (optionIdList == null || optionIdList.isEmpty()) return;

        queryFactory
                .delete(itemOption)
                .where(itemOption.item.id.eq(itemId)
                        .and(itemOption.id.in(optionIdList)))
                .execute();
    }

    @Override
    public List<ItemOption> findByItemIdWithLock(Long itemId) {
        QItemOption itemOption = QItemOption.itemOption;
        return queryFactory
                .selectFrom(itemOption)
                .where(itemOption.item.id.eq(itemId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }
}
