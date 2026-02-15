package kwh.cofshop.item.repository.custom;

import kwh.cofshop.item.domain.Category;

import java.util.List;

public interface CategoryRepositoryCustom {

    List<Category> findAllCategoryWithChild();

    boolean existsByParentCategoryId(Long parentId); // isLeafCategory 용

    List<Category> findByParentCategoryId(Long parentId);
}
