package kwh.cofshop.payment.repository;

import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.repository.projection.PaymentProviderLookupProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByIdAndMemberId(Long id, Long memberId);

    Optional<PaymentProviderLookupProjection> findByImpUidAndMemberId(String impUid, Long memberId);

    @Query("SELECT p.status FROM PaymentEntity p WHERE p.orderId = :orderId")
    Optional<PaymentStatus> findStatusByOrderId(@Param("orderId") Long orderId);

    Optional<PaymentEntity> findByOrderId(Long orderId);

}
