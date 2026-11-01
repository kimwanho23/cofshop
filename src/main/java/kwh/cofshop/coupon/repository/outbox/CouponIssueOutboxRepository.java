package kwh.cofshop.coupon.repository.outbox;

import kwh.cofshop.coupon.repository.outbox.entity.CouponIssueOutboxEvent;
import kwh.cofshop.coupon.repository.outbox.entity.CouponIssueOutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CouponIssueOutboxRepository extends JpaRepository<CouponIssueOutboxEvent, Long> {
    Page<CouponIssueOutboxEvent> findByStatusOrderByIdAsc(CouponIssueOutboxStatus status, Pageable pageable);

    long countByStatus(CouponIssueOutboxStatus status);

    @Query("""
            select e.id
            from CouponIssueOutboxEvent e
            where e.status = :status and e.publishedAt < :cutoff
            order by e.id asc
            """)
    List<Long> findIdsPublishedBefore(@Param("status") CouponIssueOutboxStatus status,
                                      @Param("cutoff") LocalDateTime cutoff,
                                      Pageable pageable);
}
