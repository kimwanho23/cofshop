package kwh.cofshop.item.repository.query;

import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.dto.response.CategoryResponseDto;

import java.util.Optional;

import java.util.List;

public interface CategoryRepositoryCustom {

    List<Category> findAllCategoryWithChild();

    Optional<CategoryResponseDto> findCategoryResponseById(Long categoryId);

    List<CategoryResponseDto> findChildCategoryResponses(Long parentId);

    List<CategoryResponseDto> findAllCategoryResponses();

    boolean existsByParentCategoryId(Long parentId); // isLeafCategory ??
}
