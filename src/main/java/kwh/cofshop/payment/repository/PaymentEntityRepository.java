package kwh.cofshop.payment.repository;

import kwh.cofshop.payment.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByIdAndMember_Id(Long id, Long memberId);

    Optional<PaymentEntity> findByImpUidAndMember_Id(String impUid, Long memberId);

}
