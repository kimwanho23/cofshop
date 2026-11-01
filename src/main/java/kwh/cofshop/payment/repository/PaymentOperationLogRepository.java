package kwh.cofshop.payment.repository;

import kwh.cofshop.payment.domain.PaymentOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentOperationLogRepository extends JpaRepository<PaymentOperationLog, Long> {

    List<PaymentOperationLog> findTop50ByPaymentIdOrderByIdDesc(Long paymentId);

    List<PaymentOperationLog> findTop50ByActionOrderByIdDesc(PaymentOperationLog.Action action);

    boolean existsByPaymentIdAndAction(Long paymentId, PaymentOperationLog.Action action);
}
