package kwh.cofshop.item.repository;

import jakarta.persistence.LockModeType;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.custom.ItemOptionRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemOptionRepository extends JpaRepository<ItemOption, Long>, ItemOptionRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ItemOption> findByItemId(Long id);

}
