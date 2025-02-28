package kwh.cofshop.item.repository;

import jakarta.persistence.LockModeType;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.repository.custom.ItemImgRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ItemImgRepository extends JpaRepository<ItemImg, Long>, ItemImgRepositoryCustom {

    @Lock(LockModeType.OPTIMISTIC)
    List<ItemImg> findByItemId(Long id);




}
