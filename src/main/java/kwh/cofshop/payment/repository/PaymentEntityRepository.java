package kwh.cofshop.payment.repository;

import kwh.cofshop.payment.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, Long> {

    PaymentEntity findByMerchantUidAndMemberId(String merchantUid, Long memberId);

}
