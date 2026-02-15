package kwh.cofshop.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentVerifyRequestDto {
    @NotBlank
    @Size(max = 100)
    private String impUid; // PortOne v2 transactionId (v1 imp_uid)

    @NotBlank
    @Size(max = 100)
    private String merchantUid;

    @NotNull
    @Positive
    private Long amount; // 금액이 일치하는지 검증
}
