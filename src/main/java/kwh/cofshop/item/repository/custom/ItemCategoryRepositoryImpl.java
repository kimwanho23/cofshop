package kwh.cofshop.item.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kwh.cofshop.item.domain.ItemCategory;
import kwh.cofshop.item.domain.QItemCategory;
import kwh.cofshop.item.domain.QItemOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ItemCategoryRepositoryImpl implements ItemCategoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;


    @Override
    public void deleteByItemIdAndCategoryIds(Long itemId, List<Long> categoryIds) {
        QItemCategory itemCategory = QItemCategory.itemCategory;
        if (categoryIds == null || categoryIds.isEmpty()) return; // 예외 처리

        queryFactory
                .delete(itemCategory)
                .where(itemCategory.item.id.eq(itemId)
                        .and(itemCategory.category.id.in(categoryIds)))
                .execute();
    }

    @Override
    public List<ItemCategory> findByItemIdWithLock(Long itemId) {
        QItemCategory itemCategory = QItemCategory.itemCategory;
        return queryFactory
                .selectFrom(itemCategory)
                .where(itemCategory.item.id.eq(itemId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }

}
