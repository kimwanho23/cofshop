package kwh.cofshop.item.repository.query;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.domain.QCategory;
import kwh.cofshop.item.dto.response.CategoryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Category> findAllCategoryWithChild() {
        QCategory category = QCategory.category;

        return queryFactory.selectFrom(category).distinct()
                .leftJoin(category.children).fetchJoin() // ?먯떇 移댄뀒怨좊━瑜???踰덉뿉 媛?몄삤湲?
                .where(category.parent.isNull()) // 理쒖긽??遺紐?移댄뀒怨좊━留?媛?몄삤湲?
                .fetch();
    }

    @Override
    public Optional<CategoryResponseDto> findCategoryResponseById(Long categoryId) {
        QCategory category = QCategory.category;

        return Optional.ofNullable(queryFactory
                .select(Projections.fields(
                        CategoryResponseDto.class,
                        category.id,
                        category.parent.id.as("parentCategoryId"),
                        category.name,
                        category.depth
                ))
                .from(category)
                .where(category.id.eq(categoryId))
                .fetchOne());
    }

    @Override
    public List<CategoryResponseDto> findChildCategoryResponses(Long parentId) {
        QCategory category = QCategory.category;

        return queryFactory
                .select(Projections.fields(
                        CategoryResponseDto.class,
                        category.id,
                        category.parent.id.as("parentCategoryId"),
                        category.name,
                        category.depth
                ))
                .from(category)
                .where(category.parent.id.eq(parentId))
                .orderBy(category.id.asc())
                .fetch();
    }

    @Override
    public List<CategoryResponseDto> findAllCategoryResponses() {
        QCategory category = QCategory.category;

        return queryFactory
                .select(Projections.fields(
                        CategoryResponseDto.class,
                        category.id,
                        category.parent.id.as("parentCategoryId"),
                        category.name,
                        category.depth
                ))
                .from(category)
                .orderBy(category.id.asc())
                .fetch();
    }

    @Override
    public boolean existsByParentCategoryId(Long parentId) {
        QCategory category = QCategory.category;
        Integer fetchOne = queryFactory
                .selectOne()
                .from(category)
                .where(category.parent.id.eq(parentId))
                .fetchFirst(); // limit 1

        return fetchOne != null;
    }
}
