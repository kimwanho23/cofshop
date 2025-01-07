package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.ItemOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemOptionRepository extends JpaRepository<ItemOption, Long> {

    //List<ItemOption> findByItemId(Long itemId);
}
