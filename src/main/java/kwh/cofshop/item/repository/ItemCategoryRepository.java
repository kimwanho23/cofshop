package kwh.cofshop.item.repository;

import jakarta.persistence.LockModeType;
import kwh.cofshop.item.domain.ItemCategory;
import kwh.cofshop.item.repository.query.ItemCategoryRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long>, ItemCategoryRepositoryCustom {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ItemCategory> findByItemId(Long id);
}
