package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.ItemImg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemImgRepository extends JpaRepository<ItemImg, Long> {
}
