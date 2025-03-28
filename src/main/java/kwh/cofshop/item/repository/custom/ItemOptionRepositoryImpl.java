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
    public Optional<ItemOption> findByIdWithLock(Long optionId) {
        QItemOption itemOption = QItemOption.itemOption;

        ItemOption result = queryFactory
                .selectFrom(itemOption)
                .where(itemOption.id.eq(optionId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)  // 비관적 락 설정
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public void deleteByItemIdAndId(Long itemId, List<Long> ids) {
        QItemOption itemOption = QItemOption.itemOption;
        if (ids == null || ids.isEmpty()) return;

        queryFactory
                .delete(itemOption)
                .where(itemOption.item.id.eq(itemId)
                        .and(itemOption.id.in(ids)))
                .execute();
    }
}
