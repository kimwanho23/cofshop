package kwh.cofshop.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRefundRequestDto {
    @NotNull
    private String merchantUid;

    @NotNull
    private Long amount;
}
