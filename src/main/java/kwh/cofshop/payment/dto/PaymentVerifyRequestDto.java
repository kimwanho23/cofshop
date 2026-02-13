package kwh.cofshop.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentVerifyRequestDto {
    @NotBlank
    private String impUid; // PortOne v2 transactionId (v1 imp_uid)

    @NotBlank
    private String merchantUid;

    @NotNull
    @Positive
    private Long amount; // 금액이 일치하는지 검증
}
