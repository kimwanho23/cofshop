package kwh.cofshop.order.service;

import kwh.cofshop.coupon.api.CouponApi;
import kwh.cofshop.member.api.MemberPointPort;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.api.OrderPaymentPreparePort;
import kwh.cofshop.order.api.OrderPaymentStatusPort;
import kwh.cofshop.order.api.OrderRefundPort;
import kwh.cofshop.order.api.OrderStatePort;
import kwh.cofshop.order.api.OrderStatus;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.mapper.OrderMapper;
import kwh.cofshop.order.policy.DeliveryFeePolicy;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.payment.client.portone.PortOneCancellation;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.dto.request.PaymentRefundRequestDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import kwh.cofshop.payment.service.PaymentProviderService;
import kwh.cofshop.payment.service.PaymentRefundTxService;
import kwh.cofshop.payment.service.PaymentService;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundFlowIntegrationTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private DeliveryFeePolicy deliveryFeePolicy;
    @Mock
    private MemberReadPort memberReadPort;
    @Mock
    private MemberPointPort memberPointPort;
    @Mock
    private CouponApi couponApi;
    @Mock
    private OrderPaymentStatusPort orderPaymentStatusPort;

    @Mock
    private PaymentEntityRepository paymentEntityRepository;
    @Mock
    private PaymentProviderService paymentProviderService;
    @Mock
    private OrderPaymentPreparePort orderPaymentPreparePort;
    @Mock
    private OrderStatePort orderStatePort;
    @Mock
    private RefundCompensationRecoveryQueueService recoveryQueueService;

    private OrderService orderService;
    private PaymentRefundTxService paymentRefundTxService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                orderMapper,
                orderItemService,
                deliveryFeePolicy,
                memberReadPort,
                memberPointPort,
                couponApi,
                orderPaymentStatusPort
        );
        OrderRefundPort orderRefundPort = new OrderRefundPortAdapter(orderRepository, orderService);
        paymentRefundTxService = new PaymentRefundTxService(
                paymentEntityRepository,
                orderRefundPort,
                orderStatePort,
                recoveryQueueService
        );
        paymentService = new PaymentService(
                orderPaymentPreparePort,
                orderStatePort,
                orderRefundPort,
                paymentRefundTxService,
                paymentEntityRepository,
                paymentProviderService
        );
    }

    @Test
    @DisplayName("환불 E2E: 요청 승인 후 결제 환불 성공 시 주문/결제 상태가 함께 완료")
    void refundFlow_endToEnd() {
        Member member = Member.builder()
                .id(1L)
                .email("user1@example.com")
                .memberName("사용자1")
                .memberPwd("pw")
                .tel("01012341234")
                .build();

        Order order = Order.builder()
                .id(1L)
                .member(member)
                .orderState(OrderState.PAID)
                .refundRequestStatus(OrderRefundRequestStatus.REQUESTED)
                .orderItems(new ArrayList<>())
                .build();

        PaymentEntity paymentEntity = PaymentEntity.createPayment(
                1L,
                1L,
                "order-1",
                1000L,
                "user1@example.com",
                "사용자1",
                "01012341234",
                "kakaopay",
                "card"
        );
        paymentEntity.paymentSuccess("imp_1", "pg_tid", 1000L, LocalDateTime.now());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.findRefundRequestStatusById(1L)).thenReturn(Optional.of(OrderRefundRequestStatus.APPROVED));
        when(paymentEntityRepository.findByIdAndMemberId(10L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.PAID);
        when(paymentProviderService.cancelPayment("order-1"))
                .thenReturn(new PortOneCancellation("cancel_1", "SUCCEEDED"));
        when(paymentProviderService.getPayment("order-1"))
                .thenReturn(new PortOnePayment(
                        "order-1",
                        "imp_1",
                        "pg_tid",
                        "CANCELLED",
                        new PortOnePayment.Amount(1000L, 1000L)
                ));

        orderService.processRefundRequestByAdmin(1L, OrderRefundRequestStatus.APPROVED, "환불 승인");

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);
        paymentService.refundPayment(10L, 1L, requestDto);

        assertThat(paymentEntity.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(order.getOrderState()).isEqualTo(OrderState.CANCELLED);
        assertThat(order.getRefundRequestStatus()).isEqualTo(OrderRefundRequestStatus.REFUNDED);
        assertThat(order.getRefundProcessedReason()).isEqualTo("결제 환불 완료");

        verify(orderStatePort).changeOrderState(1L, OrderStatus.REFUND_PENDING);
        verify(recoveryQueueService, never()).enqueue(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
