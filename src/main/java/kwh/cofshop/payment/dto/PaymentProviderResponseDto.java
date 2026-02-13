package kwh.cofshop.payment.dto;

import kwh.cofshop.payment.client.portone.PortOnePayment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentProviderResponseDto {

    private String paymentId;
    private String transactionId;
    private String pgTxId;
    private Long paidAmount;
    private String status;

    public static PaymentProviderResponseDto from(PortOnePayment payment) {
        return PaymentProviderResponseDto.builder()
                .paymentId(payment.id())
                .transactionId(payment.transactionId())
                .pgTxId(payment.pgTxId())
                .paidAmount(payment.paidAmount())
                .status(payment.status())
                .build();
    }
}
