package kwh.cofshop.payment.repository;

import jakarta.persistence.LockModeType;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.repository.projection.PaymentProviderLookupProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByIdAndMemberId(Long id, Long memberId);

    Optional<PaymentProviderLookupProjection> findByImpUidAndMemberId(String impUid, Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.id = :id AND p.memberId = :memberId")
    Optional<PaymentEntity> findByIdAndMemberIdWithLock(@Param("id") Long id, @Param("memberId") Long memberId);

    @Query("SELECT p.status FROM PaymentEntity p WHERE p.orderId = :orderId")
    Optional<PaymentStatus> findStatusByOrderId(@Param("orderId") Long orderId);

    Optional<PaymentEntity> findByOrderId(Long orderId);

    // 탈퇴 트랜잭션의 회원·주문 변경이 사라지지 않도록 영속성 컨텍스트를 비우지 않는다
    @Modifying
    @Query("""
            UPDATE PaymentEntity p
            SET p.buyerEmail = NULL, p.buyerName = NULL, p.buyerTel = NULL
            WHERE p.memberId = :memberId
            """)
    int clearBuyerInfo(@Param("memberId") Long memberId);

    List<PaymentEntity> findTop50ByStatusOrderByIdAsc(PaymentStatus status);

    // 오래 확인하지 않은 건부터 조회해 미결제 건이 많아도 뒤의 건이 밀리지 않게 함
    @Query("""
            SELECT p.id
            FROM PaymentEntity p
            WHERE p.status = :status
              AND p.requestedAt BETWEEN :from AND :to
            ORDER BY COALESCE(p.lastReconciledAt, p.requestedAt) ASC
            """)
    List<Long> findReconciliationTargets(@Param("status") PaymentStatus status,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         Pageable pageable);

    @Modifying
    @Query("UPDATE PaymentEntity p SET p.lastReconciledAt = :reconciledAt WHERE p.id = :id")
    int markReconciled(@Param("id") Long id, @Param("reconciledAt") LocalDateTime reconciledAt);

}
