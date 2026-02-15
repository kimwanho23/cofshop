package kwh.cofshop.coupon.repository.outbox;

import kwh.cofshop.coupon.repository.outbox.entity.CouponIssueOutboxEvent;
import kwh.cofshop.coupon.repository.outbox.entity.CouponIssueOutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueOutboxRepository extends JpaRepository<CouponIssueOutboxEvent, Long> {
    Page<CouponIssueOutboxEvent> findByStatusOrderByIdAsc(CouponIssueOutboxStatus status, Pageable pageable);
}
