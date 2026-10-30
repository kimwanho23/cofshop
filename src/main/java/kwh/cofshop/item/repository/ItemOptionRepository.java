package kwh.cofshop.item.repository;

import jakarta.persistence.LockModeType;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.query.ItemOptionRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemOptionRepository extends JpaRepository<ItemOption, Long>, ItemOptionRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT io FROM ItemOption io WHERE io.item.id = :id ORDER BY io.id")
    List<ItemOption> findByItemId(@Param("id") Long id);

    // 데드락 방지를 위해 항상 ID 순서로 락 획득
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT io FROM ItemOption io WHERE io.id IN :ids ORDER BY io.id")
    List<ItemOption> findAllByIdInWithLock(@Param("ids") List<Long> ids);


}
