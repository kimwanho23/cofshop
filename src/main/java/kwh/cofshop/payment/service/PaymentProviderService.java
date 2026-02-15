package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.payment.client.portone.PortOneCancellation;
import kwh.cofshop.payment.client.portone.PortOneClientException;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProviderService {

    private static final String DEFAULT_REFUND_REASON = "User requested refund";

    private final PortOnePaymentClient portOnePaymentClient;

    public PortOnePayment getPayment(String merchantUid) {
        try {
            PortOnePayment payment = portOnePaymentClient.getPayment(merchantUid);
            if (payment == null) {
                throw new PortOneClientException("PortOne payment response is empty");
            }
            return payment;
        } catch (PortOneClientException e) {
            log.warn("PortOne payment lookup failed: {}", e.getMessage());
            throw new BusinessException(BusinessErrorCode.PAYMENT_PROVIDER_ERROR);
        }
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
