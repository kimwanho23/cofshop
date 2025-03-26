package kwh.cofshop.item.repository.custom;

import kwh.cofshop.item.domain.Category;

import java.util.List;

public interface CategoryRepositoryCustom {

    List<Category> findAllCategoryWithChild();
}
