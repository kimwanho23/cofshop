package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.repository.custom.CategoryRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long>, CategoryRepositoryCustom {
}
