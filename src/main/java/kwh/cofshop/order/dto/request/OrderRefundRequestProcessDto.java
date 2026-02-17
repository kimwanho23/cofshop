package kwh.cofshop.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRefundRequestProcessDto {

    @NotNull(message = "환불 요청 상태는 필수입니다.")
    private OrderRefundRequestProcessStatus refundRequestStatus;

    @Size(max = 500, message = "처리 사유는 500자 이하여야 합니다.")
    private String processReason;

    public OrderRefundRequestStatus toDomainStatus() {
        return refundRequestStatus.toDomainStatus();
    }
}
