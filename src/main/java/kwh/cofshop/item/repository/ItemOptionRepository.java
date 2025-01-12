package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.custom.ItemOptionRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemOptionRepository extends JpaRepository<ItemOption, Long>, ItemOptionRepositoryCustom {

}
