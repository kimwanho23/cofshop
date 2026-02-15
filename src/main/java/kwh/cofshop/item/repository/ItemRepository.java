package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.repository.query.ItemRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, ItemRepositoryCustom {
}
