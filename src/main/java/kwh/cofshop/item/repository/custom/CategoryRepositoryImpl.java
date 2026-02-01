package kwh.cofshop.item.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.domain.QCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Category> findAllCategoryWithChild() {
        QCategory category = QCategory.category;

        return queryFactory.selectFrom(category).distinct()
                .leftJoin(category.children).fetchJoin() // 자식 카테고리를 한 번에 가져오기
                .where(category.parent.isNull()) // 최상위 부모 카테고리만 가져오기
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

    @Override
    public List<Category> findByParentCategoryId(Long parentId) {
        QCategory category = QCategory.category;
        return queryFactory
                .selectFrom(category)
                .where(category.parent.id.eq(parentId))
                .fetch();
    }

}
