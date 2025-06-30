package kwh.cofshop.payment.dto;

import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Builder
public class PaymentResponseDto {
    private Long paymentId;          // 결제 ID
    private String merchantUid;      // 주문번호 (order-blahblah)
    private Long price;           // 최종 결제 금액
    private String pgProvider;       // PG사
    private String payMethod;        // 결제 방식
    private PaymentStatus status;           // READY, PAID, CANCELLED

    public static PaymentResponseDto from(PaymentEntity payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .merchantUid(payment.getMerchantUid())
                .price(payment.getPrice())
                .pgProvider(payment.getPgProvider())
                .payMethod(payment.getPayMethod())
                .status(payment.getStatus())
                .build();
    }

}

