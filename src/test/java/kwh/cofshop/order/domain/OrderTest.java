package kwh.cofshop.order.domain;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"WAITING_FOR_PAY", "PAYMENT_PENDING", "REFUND_PENDING", "PAID"})
    @DisplayName("취소: 배송 전 상태는 취소할 수 있고 사유와 시각이 남음")
    void cancel_allowed(OrderState state) {
        Order order = orderIn(state);

        order.cancel("변심", null);

        assertThat(order.getOrderState()).isEqualTo(OrderState.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo("변심");
        assertThat(order.getCancelledAt()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"PREPARING_FOR_SHIPMENT", "SHIPPING", "DELIVERED"})
    @DisplayName("취소: 배송이 시작되면 취소할 수 없음")
    void cancel_afterShipmentStarted(OrderState state) {
        assertError(() -> orderIn(state).cancel("변심", LocalDateTime.now()), BusinessErrorCode.ORDER_CANNOT_CANCEL);
    }

    @Test
    @DisplayName("취소: 이미 취소됐거나 구매 확정된 주문은 취소할 수 없음")
    void cancel_finished() {
        assertError(() -> orderIn(OrderState.CANCELLED).cancel("변심", null), BusinessErrorCode.ORDER_ALREADY_CANCELLED);
        assertError(() -> orderIn(OrderState.COMPLETED).cancel("변심", null), BusinessErrorCode.ORDER_ALREADY_COMPLETED);
    }

    @Test
    @DisplayName("결제 요청: 결제 대기 상태에서만 가능")
    void pay_onlyFromWaiting() {
        Order order = orderIn(OrderState.WAITING_FOR_PAY);
        order.pay();
        assertThat(order.getOrderState()).isEqualTo(OrderState.PAYMENT_PENDING);

        assertError(() -> orderIn(OrderState.PAID).pay(), BusinessErrorCode.PAYMENT_FAIL);
    }

    @Test
    @DisplayName("배송 완료: 처음 완료될 때의 시각을 유지함")
    void changeState_keepsFirstDeliveredAt() {
        Order order = orderIn(OrderState.SHIPPING);
        order.changeOrderState(OrderState.DELIVERED);
        LocalDateTime deliveredAt = order.getDeliveredAt();
        assertThat(deliveredAt).isNotNull();

        ReflectionTestUtils.setField(order, "deliveredAt", deliveredAt.minusDays(1));
        order.changeOrderState(OrderState.DELIVERED);

        assertThat(order.getDeliveredAt()).isEqualTo(deliveredAt.minusDays(1));
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"PAID", "PREPARING_FOR_SHIPMENT", "SHIPPING", "DELIVERED"})
    @DisplayName("환불 요청: 결제 후 구매 확정 전까지 가능")
    void refundRequest_allowed(OrderState state) {
        Order order = orderIn(state);

        order.requestRefundRequest("불량", null);

        assertThat(order.getRefundRequestStatus()).isEqualTo(OrderRefundRequestStatus.REQUESTED);
        assertThat(order.getRefundRequestedAt()).isNotNull();
        assertThat(order.isRefundRequestInProgress()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"WAITING_FOR_PAY", "PAYMENT_PENDING", "CANCELLED", "REFUND_PENDING"})
    @DisplayName("환불 요청: 그 외 상태는 불가")
    void refundRequest_notAllowed(OrderState state) {
        assertError(() -> orderIn(state).requestRefundRequest("불량", null), BusinessErrorCode.ORDER_REFUND_REQUEST_NOT_ALLOWED);
    }

    @Test
    @DisplayName("환불 요청: 구매 확정 후에도 배송 완료 3개월까지 가능")
    void refundRequest_afterCompleted() {
        LocalDateTime deliveredAt = LocalDateTime.of(2026, 1, 15, 12, 0);
        Order order = orderIn(OrderState.COMPLETED);
        ReflectionTestUtils.setField(order, "deliveredAt", deliveredAt);

        assertError(() -> order.requestRefundRequest("불량", deliveredAt.plusMonths(3).plusSeconds(1)),
                BusinessErrorCode.ORDER_REFUND_REQUEST_NOT_ALLOWED);

        order.requestRefundRequest("불량", deliveredAt.plusMonths(3));
        assertThat(order.getRefundRequestStatus()).isEqualTo(OrderRefundRequestStatus.REQUESTED);
    }

    @Test
    @DisplayName("환불 요청: 배송 완료 시각이 없는 구매 확정 주문은 고객에게 불리하지 않게 허용")
    void refundRequest_completedWithoutDeliveredAt() {
        Order order = orderIn(OrderState.COMPLETED);

        order.requestRefundRequest("불량", null);

        assertThat(order.getRefundRequestStatus()).isEqualTo(OrderRefundRequestStatus.REQUESTED);
    }

    @Test
    @DisplayName("환불 시작: 구매 확정 주문은 실패 시 구매 확정으로 돌아갈 수 있게 기억함")
    void beginRefund_fromCompleted() {
        Order order = orderIn(OrderState.COMPLETED);

        order.beginRefund();

        assertThat(order.getStateBeforeRefund()).isEqualTo(OrderState.COMPLETED);
    }

    @Test
    @DisplayName("개인정보 삭제: 배송지와 요청 사항을 지움")
    void clearPersonalInfo() {
        Order order = orderIn(OrderState.COMPLETED);
        ReflectionTestUtils.setField(order, "address", Address.builder().city("서울").street("길").zipCode("00000").build());
        ReflectionTestUtils.setField(order, "deliveryRequest", "문 앞");

        order.clearPersonalInfo();

        assertThat(order.getAddress()).isNull();
        assertThat(order.getDeliveryRequest()).isNull();
    }

    @Test
    @DisplayName("환불 요청: 진행 중이거나 환불 완료면 다시 요청할 수 없고, 거절 후에는 다시 요청 가능")
    void refundRequest_again() {
        assertError(() -> refundIn(OrderRefundRequestStatus.REQUESTED).requestRefundRequest("또", null),
                BusinessErrorCode.ORDER_REFUND_REQUEST_ALREADY_REQUESTED);
        assertError(() -> refundIn(OrderRefundRequestStatus.APPROVED).requestRefundRequest("또", null),
                BusinessErrorCode.ORDER_REFUND_REQUEST_ALREADY_REQUESTED);
        assertError(() -> refundIn(OrderRefundRequestStatus.REFUNDED).requestRefundRequest("또", null),
                BusinessErrorCode.ORDER_REFUND_REQUEST_INVALID_STATE_TRANSITION);

        Order rejected = refundIn(OrderRefundRequestStatus.REJECTED);
        rejected.requestRefundRequest("다시", null);
        assertThat(rejected.getRefundRequestStatus()).isEqualTo(OrderRefundRequestStatus.REQUESTED);
        assertThat(rejected.getRefundProcessedAt()).isNull();
    }

    @Test
    @DisplayName("환불 처리: 요청 -> 승인·거절, 승인 -> 완료만 허용")
    void processRefund_transitions() {
        Order approved = refundIn(OrderRefundRequestStatus.REQUESTED);
        approved.processRefundRequest(OrderRefundRequestStatus.APPROVED, "승인", null);
        assertThat(approved.getRefundProcessedAt()).isNotNull();
        approved.processRefundRequest(OrderRefundRequestStatus.REFUNDED, "완료", LocalDateTime.now());
        assertThat(approved.getRefundRequestStatus()).isEqualTo(OrderRefundRequestStatus.REFUNDED);
        assertThat(approved.isRefundRequestInProgress()).isFalse();

        refundIn(OrderRefundRequestStatus.REQUESTED).processRefundRequest(OrderRefundRequestStatus.REJECTED, "거절", null);

        BusinessErrorCode invalid = BusinessErrorCode.ORDER_REFUND_REQUEST_INVALID_STATE_TRANSITION;
        assertError(() -> refundIn(OrderRefundRequestStatus.REQUESTED).processRefundRequest(OrderRefundRequestStatus.REFUNDED, "x", null), invalid);
        assertError(() -> refundIn(OrderRefundRequestStatus.REQUESTED).processRefundRequest(null, "x", null), invalid);
        assertError(() -> refundIn(OrderRefundRequestStatus.APPROVED).processRefundRequest(OrderRefundRequestStatus.REJECTED, "x", null), invalid);
        assertError(() -> refundIn(OrderRefundRequestStatus.REJECTED).processRefundRequest(OrderRefundRequestStatus.APPROVED, "x", null), invalid);
        assertError(() -> refundIn(OrderRefundRequestStatus.REFUNDED).processRefundRequest(OrderRefundRequestStatus.REJECTED, "x", null), invalid);
        assertError(() -> orderIn(OrderState.PAID).processRefundRequest(OrderRefundRequestStatus.APPROVED, "x", null),
                BusinessErrorCode.ORDER_REFUND_REQUEST_NOT_REQUESTED);
    }

    @Test
    @DisplayName("환불 시작: 이전 상태를 기억하고, 두 번 호출해도 덮어쓰지 않음")
    void beginRefund_keepsPreviousState() {
        Order order = orderIn(OrderState.SHIPPING);

        order.beginRefund();
        order.beginRefund();

        assertThat(order.getOrderState()).isEqualTo(OrderState.REFUND_PENDING);
        assertThat(order.getStateBeforeRefund()).isEqualTo(OrderState.SHIPPING);
    }

    private Order orderIn(OrderState state) {
        Order order = Order.builder().build();
        ReflectionTestUtils.setField(order, "orderState", state);
        return order;
    }

    private Order refundIn(OrderRefundRequestStatus status) {
        Order order = orderIn(OrderState.PAID);
        ReflectionTestUtils.setField(order, "refundRequestStatus", status);
        return order;
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, BusinessErrorCode expected) {
        assertThatThrownBy(action)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }
}
