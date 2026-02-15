package kwh.cofshop.coupon.infrastructure.outbox.persistence.repository;

import kwh.cofshop.coupon.infrastructure.outbox.persistence.entity.CouponIssueOutboxEvent;
import kwh.cofshop.coupon.infrastructure.outbox.persistence.entity.CouponIssueOutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueOutboxRepository extends JpaRepository<CouponIssueOutboxEvent, Long> {
    Page<CouponIssueOutboxEvent> findByStatusOrderByIdAsc(CouponIssueOutboxStatus status, Pageable pageable);
}
