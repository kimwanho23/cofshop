package kwh.cofshop.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRefundRequestDto {

    @NotBlank(message = "환불 요청 사유는 필수입니다.")
    @Size(max = 500, message = "환불 요청 사유는 500자 이하여야 합니다.")
    private String refundRequestReason;
}
