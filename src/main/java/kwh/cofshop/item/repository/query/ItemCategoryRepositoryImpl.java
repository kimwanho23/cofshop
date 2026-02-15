package kwh.cofshop.item.repository.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.QItemCategory;
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
        if (categoryIds == null || categoryIds.isEmpty()) return; // ?덉쇅 泥섎━

        queryFactory
                .delete(itemCategory)
                .where(itemCategory.item.id.eq(itemId)
                        .and(itemCategory.category.id.in(categoryIds)))
                .execute();
    }

}
