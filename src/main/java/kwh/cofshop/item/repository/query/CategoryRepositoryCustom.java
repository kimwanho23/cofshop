package kwh.cofshop.item.repository.query;

import kwh.cofshop.item.domain.Category;

import java.util.List;

public interface CategoryRepositoryCustom {

    List<Category> findAllCategoryWithChild();

    boolean existsByParentCategoryId(Long parentId); // isLeafCategory ??
}
