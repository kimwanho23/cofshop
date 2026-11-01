package kwh.cofshop.payment.client.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// POST /payments/{paymentId}/cancel 응답. 취소 정보는 cancellation 필드 안에 있음
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneCancelResponse(
        PortOneCancellation cancellation
) {
}
