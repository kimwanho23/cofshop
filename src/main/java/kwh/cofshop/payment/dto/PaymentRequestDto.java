package kwh.cofshop.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentRequestDto {

    @NotBlank
    private String impUid; // 아임포트 결제 고유 ID

    @NotBlank
    private String merchantUid; // 고유 주문번호

    @NotBlank
    private String pgProvider; // PG사

    @NotBlank
    private String payMethod; // 결제 수단

    @NotNull
    @Positive
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
