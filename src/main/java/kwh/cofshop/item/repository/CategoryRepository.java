package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.dto.CategoryPathDto;
import kwh.cofshop.item.repository.custom.CategoryRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long>, CategoryRepositoryCustom {

    @Query(value = """
        WITH RECURSIVE category_path AS (
            SELECT id, name, parent_category_id
            FROM category
            WHERE id = :categoryId

            UNION ALL

            SELECT c.id, c.name, c.parent_category_id
            FROM category c
            INNER JOIN category_path cp ON c.id = cp.parent_category_id
        )
        SELECT id, name, parent_category_id AS parentCategoryId
        FROM category_path
        """, nativeQuery = true)
    List<CategoryPathDto> findCategoryPath(@Param("categoryId") Long categoryId);

    @Query(value = "SELECT * FROM category WHERE parent_category_id = :parentId", nativeQuery = true)
    List<Category> findImmediateChildrenNative(@Param("parentId") Long parentId);
}
