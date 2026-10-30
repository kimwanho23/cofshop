package kwh.cofshop.payment.service;

import io.micrometer.core.instrument.Metrics;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.payment.client.portone.PortOneCancellation;
import kwh.cofshop.payment.client.portone.PortOneClientException;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
import kwh.cofshop.payment.client.portone.PortOnePaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProviderService {

    private static final String DEFAULT_REFUND_REASON = "User requested refund";

    private final PortOnePaymentClient portOnePaymentClient;

    public PortOnePayment getPayment(String merchantUid) {
        try {
            return getPaymentFromClient(merchantUid);
        } catch (PortOneClientException e) {
            log.warn("PortOne payment lookup failed: {}", e.getMessage());
            Metrics.counter("payment.provider.failures", "operation", "lookup").increment();
            throw new BusinessException(BusinessErrorCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    // PortOne에 결제 건이 없으면 빈 값. 조회 자체가 실패하면 예외
    public Optional<PortOnePayment> findPayment(String merchantUid) {
        try {
            return Optional.of(getPaymentFromClient(merchantUid));
        } catch (PortOnePaymentNotFoundException e) {
            return Optional.empty();
        } catch (PortOneClientException e) {
            log.warn("PortOne payment lookup failed: {}", e.getMessage());
            Metrics.counter("payment.provider.failures", "operation", "lookup").increment();
            throw new BusinessException(BusinessErrorCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    private PortOnePayment getPaymentFromClient(String merchantUid) {
        PortOnePayment payment = portOnePaymentClient.getPayment(merchantUid);
        if (payment == null) {
            throw new PortOneClientException("PortOne payment response is empty");
        }
        return payment;
    }

    public PortOneCancellation cancelPayment(String merchantUid) {
        try {
            PortOneCancellation cancellation = portOnePaymentClient.cancelPayment(merchantUid, DEFAULT_REFUND_REASON);
            if (cancellation == null) {
                throw new PortOneClientException("PortOne cancel response is empty");
            }
            return cancellation;
        } catch (PortOneClientException e) {
            log.warn("PortOne payment cancel failed: {}", e.getMessage());
            Metrics.counter("payment.provider.failures", "operation", "cancel").increment();
            throw new BusinessException(BusinessErrorCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    public boolean isPaymentCancelled(String merchantUid) {
        try {
            PortOnePayment payment = getPayment(merchantUid);
            return "CANCELLED".equalsIgnoreCase(payment.status());
        } catch (BusinessException ignored) {
            return false;
        }
    }
}
