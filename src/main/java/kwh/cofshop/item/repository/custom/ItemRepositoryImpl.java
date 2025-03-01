package kwh.cofshop.item.repository.custom;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.QCategory;
import kwh.cofshop.item.domain.QItem;
import kwh.cofshop.item.domain.QItemCategory;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

import static kwh.cofshop.item.domain.QItem.item;
import static kwh.cofshop.item.domain.QCategory.category;
import static kwh.cofshop.item.domain.QItemCategory.itemCategory;

@Repository
@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Item> searchItems(ItemSearchRequestDto requestDto, Pageable pageable) {
        List<Item> result = queryFactory
                .selectFrom(item)
                .leftJoin(itemCategory).on(itemCategory.item.eq(item))
                .where(nameCondition(requestDto.getItemName()),
                        categoryCondition(requestDto.getCategoryId()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 총 개수 계산
        JPAQuery<Long> totalCount = queryFactory
                .select(item.count())
                .from(item)
                .leftJoin(itemCategory).on(itemCategory.item.eq(item))
                .where(nameCondition(requestDto.getItemName()),
                        categoryCondition(requestDto.getCategoryId()));

        return PageableExecutionUtils.getPage(result, pageable, totalCount::fetchOne);
    }

    private BooleanExpression nameCondition(String itemName) {
        return StringUtils.hasText(itemName) ? QItem.item.itemName.containsIgnoreCase(itemName) : null;
    }

    private BooleanExpression categoryCondition(Long categoryId) {
        if (categoryId == null) {
            return null;
        }

        List<Long> categoryIds = queryFactory
                .select(category.id)
                .from(category)
                .where(category.id.eq(categoryId)
                        .or(category.parent.id.eq(categoryId)))
                .fetch();

        return categoryIds.isEmpty() ? null : itemCategory.category.id.in(categoryIds);
    }


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
