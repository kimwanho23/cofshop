package kwh.cofshop.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentRequestDto {

    @NotNull
    private String impUid; // 포트원 결제 고유 ID

    @NotNull
    private String merchantUid; // 고유 주문번호

    @NotNull
    private String pgProvider; // PG사

    @NotNull
    private String payMethod; // 결제 수단

    private Long amount; // 금액

    @Builder
    public PaymentRequestDto(String impUid, String merchantUid, String pgProvider, String payMethod, Long amount) {
        this.impUid = impUid;
        this.merchantUid = merchantUid;
        this.pgProvider = pgProvider;
        this.payMethod = payMethod;
        this.amount = amount;
    }
}