package kwh.cofshop.item.repository;

import jakarta.persistence.LockModeType;
import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.repository.custom.ItemImgRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemImgRepository extends JpaRepository<ItemImg, Long>, ItemImgRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ItemImg> findByItemId(Long id);

}
