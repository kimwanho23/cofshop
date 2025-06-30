package kwh.cofshop.payment.repository;

import kwh.cofshop.payment.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, Long> {

    PaymentEntity findByMerchantUidAndMemberId(String merchantUid, Long memberId);

    Optional<PaymentEntity> findByImpUid(String impUid);
}
