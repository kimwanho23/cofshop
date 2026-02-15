package kwh.cofshop.item.repository.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.QItemOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ItemOptionRepositoryImpl implements ItemOptionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public void deleteByItemIdAndItemOptionId(Long itemId, List<Long> optionIdList) {
        if (optionIdList == null || optionIdList.isEmpty()) {
            return;
        }

        QItemOption itemOption = QItemOption.itemOption;
        queryFactory
                .delete(itemOption)
                .where(itemOption.item.id.eq(itemId)
                        .and(itemOption.id.in(optionIdList)))
                .execute();
    }
}
