package kwh.cofshop.order.dto.request;

import kwh.cofshop.order.domain.OrderRefundRequestStatus;

public enum OrderRefundRequestProcessStatus {
    APPROVED,
    REJECTED;

    public OrderRefundRequestStatus toDomainStatus() {
        return switch (this) {
            case APPROVED -> OrderRefundRequestStatus.APPROVED;
            case REJECTED -> OrderRefundRequestStatus.REJECTED;
        };
    }
}
