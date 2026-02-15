package kwh.cofshop.payment.repository;

import kwh.cofshop.payment.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByIdAndMemberId(Long id, Long memberId);

    Optional<PaymentEntity> findByImpUidAndMemberId(String impUid, Long memberId);

    Optional<PaymentEntity> findByOrderId(Long orderId);

}
