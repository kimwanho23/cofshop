package kwh.cofshop.item.repository.query;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.QItemImg;
import kwh.cofshop.item.domain.QItemOption;
import kwh.cofshop.item.domain.QItemCategory;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import kwh.cofshop.item.dto.response.ItemImgResponseDto;
import kwh.cofshop.item.dto.response.ItemOptionResponseDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static kwh.cofshop.item.domain.QCategory.category;
import static kwh.cofshop.item.domain.QItem.item;
import static kwh.cofshop.item.domain.QItemCategory.itemCategory;

@Repository
@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ItemSearchResponseDto> searchItems(ItemSearchRequestDto requestDto, Pageable pageable) {
        QItemCategory primaryCategory = new QItemCategory("primaryCategory");

        List<ItemSearchResponseDto> result = queryFactory
                .select(Projections.fields(
                        ItemSearchResponseDto.class,
                        item.itemName.as("itemName"),
                        item.price.as("price"),
                        item.discount.as("discount"),
                        item.deliveryFee.as("deliveryFee"),
                        item.itemState.as("itemState"),
                        ExpressionUtils.as(
                                JPAExpressions.select(primaryCategory.category.id.min())
                                        .from(primaryCategory)
                                        .where(primaryCategory.item.id.eq(item.id)),
                                "categoryId"
                        )
                ))
                .from(item)
                .where(nameCondition(requestDto.getItemName()),
                        categoryCondition(requestDto.getCategoryId()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> totalCount = queryFactory
                .select(item.count())
                .from(item)
                .where(nameCondition(requestDto.getItemName()),
                        categoryCondition(requestDto.getCategoryId()));

        return PageableExecutionUtils.getPage(result, pageable, totalCount::fetchOne);
    }

    @Override
    public Optional<ItemResponseDto> findItemResponseById(Long itemId) {
        return findItemResponsesByIds(List.of(itemId)).stream().findFirst();
    }

    @Override
    public List<ItemResponseDto> findItemResponsesByIds(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }

        List<Long> uniqueItemIds = new ArrayList<>(new LinkedHashSet<>(itemIds));

        QItemCategory primaryCategory = new QItemCategory("primaryCategory");
        List<ItemResponseDto> baseRows = queryFactory
                .select(Projections.fields(
                        ItemResponseDto.class,
                        item.id,
                        item.itemName,
                        item.price,
                        item.discount,
                        item.deliveryFee,
                        item.origin,
                        item.itemLimit,
                        item.itemState,
                        item.seller.email.as("email"),
                        ExpressionUtils.as(
                                JPAExpressions.select(primaryCategory.category.id.min())
                                        .from(primaryCategory)
                                        .where(primaryCategory.item.id.eq(item.id)),
                                "categoryId"
                        )
                ))
                .from(item)
                .where(item.id.in(uniqueItemIds))
                .fetch();

        Map<Long, ItemResponseDto> itemById = baseRows.stream()
                .collect(Collectors.toMap(
                        ItemResponseDto::getId,
                        value -> value,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        if (itemById.isEmpty()) {
            return List.of();
        }

        itemById.values().forEach(this::initializeCollections);
        appendCategoryNames(itemById, uniqueItemIds);
        appendItemImages(itemById, uniqueItemIds);
        appendItemOptions(itemById, uniqueItemIds);

        return uniqueItemIds.stream()
                .map(itemById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private void initializeCollections(ItemResponseDto itemResponseDto) {
        itemResponseDto.setCategoryNames(new ArrayList<>());
        itemResponseDto.setItemImages(new ArrayList<>());
        itemResponseDto.setItemOptions(new ArrayList<>());
    }

    private void appendCategoryNames(Map<Long, ItemResponseDto> itemById, List<Long> itemIds) {
        List<Tuple> categoryRows = queryFactory
                .select(itemCategory.item.id, category.name)
                .from(itemCategory)
                .join(itemCategory.category, category)
                .where(itemCategory.item.id.in(itemIds))
                .orderBy(itemCategory.item.id.asc(), category.id.asc())
                .fetch();

        for (Tuple row : categoryRows) {
            Long itemId = row.get(itemCategory.item.id);
            String categoryName = row.get(category.name);
            ItemResponseDto itemResponseDto = itemById.get(itemId);
            if (itemResponseDto != null && categoryName != null) {
                itemResponseDto.getCategoryNames().add(categoryName);
            }
        }
    }

    private void appendItemImages(Map<Long, ItemResponseDto> itemById, List<Long> itemIds) {
        QItemImg itemImg = QItemImg.itemImg;
        List<Tuple> imageRows = queryFactory
                .select(
                        itemImg.item.id,
                        Projections.fields(
                                ItemImgResponseDto.class,
                                itemImg.id.as("id"),
                                itemImg.imgName,
                                itemImg.oriImgName,
                                itemImg.imgUrl,
                                itemImg.imgType
                        )
                )
                .from(itemImg)
                .where(itemImg.item.id.in(itemIds))
                .orderBy(itemImg.item.id.asc(), itemImg.id.asc())
                .fetch();

        for (Tuple row : imageRows) {
            Long itemId = row.get(itemImg.item.id);
            ItemImgResponseDto imageDto = row.get(1, ItemImgResponseDto.class);
            ItemResponseDto itemResponseDto = itemById.get(itemId);
            if (itemResponseDto != null && imageDto != null) {
                itemResponseDto.getItemImages().add(imageDto);
            }
        }
    }

    private void appendItemOptions(Map<Long, ItemResponseDto> itemById, List<Long> itemIds) {
        QItemOption itemOption = QItemOption.itemOption;
        List<Tuple> optionRows = queryFactory
                .select(
                        itemOption.item.id,
                        Projections.fields(
                                ItemOptionResponseDto.class,
                                itemOption.id.as("id"),
                                itemOption.description,
                                itemOption.additionalPrice,
                                itemOption.stock
                        )
                )
                .from(itemOption)
                .where(itemOption.item.id.in(itemIds))
                .orderBy(itemOption.item.id.asc(), itemOption.id.asc())
                .fetch();

        for (Tuple row : optionRows) {
            Long itemId = row.get(itemOption.item.id);
            ItemOptionResponseDto optionDto = row.get(1, ItemOptionResponseDto.class);
            ItemResponseDto itemResponseDto = itemById.get(itemId);
            if (itemResponseDto != null && optionDto != null) {
                itemResponseDto.getItemOptions().add(optionDto);
            }
        }
    }

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

        QItemCategory filterItemCategory = new QItemCategory("filterItemCategory");
        return JPAExpressions
                .selectOne()
                .from(filterItemCategory)
                .where(
                        filterItemCategory.item.id.eq(item.id),
                        filterItemCategory.category.id.in(categoryIds)
                )
                .exists();
    }
}
