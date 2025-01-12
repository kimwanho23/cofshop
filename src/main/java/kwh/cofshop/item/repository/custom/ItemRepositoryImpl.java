package kwh.cofshop.item.repository.custom;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.QItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Item> findByItemName(String itemName, Pageable pageable) {
        QItem item = QItem.item;

        List<Item> content = queryFactory
                .selectFrom(item)
                .where(item.itemName.containsIgnoreCase(itemName))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = Optional.ofNullable(
                queryFactory.select(Wildcard.count)
                        .from(item)
                        .where(item.itemName.containsIgnoreCase(itemName))
                        .fetchOne()
        ).orElse(0L);


        return new PageImpl<>(content, pageable, total);
    }
}
