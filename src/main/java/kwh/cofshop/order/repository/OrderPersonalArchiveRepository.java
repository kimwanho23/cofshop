package kwh.cofshop.order.repository;

import kwh.cofshop.order.domain.OrderPersonalArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OrderPersonalArchiveRepository extends JpaRepository<OrderPersonalArchive, Long> {

    @Modifying
    @Query("DELETE FROM OrderPersonalArchive a WHERE a.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
