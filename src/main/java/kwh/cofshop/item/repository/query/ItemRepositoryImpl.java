package kwh.cofshop.item.repository.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static kwh.cofshop.item.domain.QCategory.category;
import static kwh.cofshop.item.domain.QItem.item;
import static kwh.cofshop.item.domain.QItemCategory.itemCategory;

@Repository
@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Item> searchItems(ItemSearchRequestDto requestDto, Pageable pageable) {
        List<Item> result = queryFactory
                .selectDistinct(item)
                .from(item)
                .leftJoin(itemCategory).on(itemCategory.item.eq(item))
                .where(nameCondition(requestDto.getItemName()),
                        categoryCondition(requestDto.getCategoryId()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 珥?媛쒖닔 怨꾩궛
        JPAQuery<Long> totalCount = queryFactory
                .select(item.id.countDistinct())
                .from(item)
                .leftJoin(itemCategory).on(itemCategory.item.eq(item))
                .where(nameCondition(requestDto.getItemName()),
                        categoryCondition(requestDto.getCategoryId()));

        return PageableExecutionUtils.getPage(result, pageable, totalCount::fetchOne);
    }

    // Full Text Index 湲곕컲 寃??
    private BooleanExpression nameCondition(String itemName) {
        return StringUtils.hasText(itemName)
                ? Expressions.booleanTemplate(
                "MATCH(item_name) AGAINST ({0} IN NATURAL LANGUAGE MODE)",
                Expressions.constant(itemName))
                : null;
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

        if (categoryIds.isEmpty()) {
            return Expressions.asBoolean(false).isTrue();
        }
        return itemCategory.category.id.in(categoryIds);
    }

}
