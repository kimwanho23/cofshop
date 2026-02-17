package kwh.cofshop.order.service;

import kwh.cofshop.coupon.api.CouponApi;
import kwh.cofshop.coupon.api.CouponApplyResult;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.member.api.MemberPointPort;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.dto.request.AddressRequestDto;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.response.OrderCancelResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.mapper.OrderMapper;
import kwh.cofshop.order.policy.DeliveryFeePolicy;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.order.api.OrderPaymentStatusPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성: 쿠폰/포인트 없음")
    void createInstanceOrder_noCouponNoPoint() {
        Member member = createMember(1L);
        when(memberReadPort.getByIdWithLock(1L)).thenReturn(member);

        ItemOption option = createOption(createItem(), 100, 10);
        when(orderItemService.getItemOptionsWithLock(any())).thenReturn(List.of(option));

        OrderItem orderItem = OrderItem.builder()
                .item(option.getItem())
                .itemOption(option)
                .orderPrice(1100)
                .discountRate(0)
                .quantity(2)
                .build();
        when(orderItemService.createOrderItems(any(), any())).thenReturn(List.of(orderItem));

        when(deliveryFeePolicy.calculate(any())).thenReturn(2500);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponseDto(any(Order.class))).thenReturn(new OrderResponseDto());

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setAddress(createAddressRequestDto());
        requestDto.setOrderItems(List.of(new OrderItemRequestDto()));
        requestDto.setUsePoint(0);

        OrderResponseDto result = orderService.createInstanceOrder(1L, requestDto);

        assertThat(result).isNotNull();
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getFinalPrice()).isEqualTo(2500 + 2200);
        assertThat(saved.getUsePoint()).isNull();
    }

    @Test
    @DisplayName("주문 생성: 쿠폰/포인트 적용")
    void createInstanceOrder_withCouponAndPoint() {
        Member member = createMember(1L);
        ReflectionTestUtils.setField(member, "point", 200);
        when(memberReadPort.getByIdWithLock(1L)).thenReturn(member);

        ItemOption option = createOption(createItem(), 100, 10);
        when(orderItemService.getItemOptionsWithLock(any())).thenReturn(List.of(option));

        OrderItem orderItem = OrderItem.builder()
                .item(option.getItem())
                .itemOption(option)
                .orderPrice(1100)
                .discountRate(0)
                .quantity(2)
                .build();
        when(orderItemService.createOrderItems(any(), any())).thenReturn(List.of(orderItem));

        when(deliveryFeePolicy.calculate(any())).thenReturn(2500);

        when(couponApi.applyCoupon(500L, 1L, 2200L))
                .thenReturn(new CouponApplyResult(500L, 500L));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponseDto(any(Order.class))).thenReturn(new OrderResponseDto());

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setAddress(createAddressRequestDto());
        requestDto.setOrderItems(List.of(new OrderItemRequestDto()));
        requestDto.setMemberCouponId(500L);
        requestDto.setUsePoint(100);

        OrderResponseDto result = orderService.createInstanceOrder(1L, requestDto);

        assertThat(result).isNotNull();
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getFinalPrice()).isEqualTo(2200 - 500 - 100 + 2500);
        assertThat(saved.getMemberCouponId()).isEqualTo(500L);
        verify(couponApi).applyCoupon(500L, 1L, 2200L);
    }

    @Test
    @DisplayName("주문 생성: couponId를 memberCouponId로 보내면 쿠폰 조회 실패")
    void createInstanceOrder_withCouponTemplateId_shouldFail() {
        Member member = createMember(1L);
        when(memberReadPort.getByIdWithLock(1L)).thenReturn(member);

        ItemOption option = createOption(createItem(), 100, 10);
        when(orderItemService.getItemOptionsWithLock(any())).thenReturn(List.of(option));

        OrderItem orderItem = OrderItem.builder()
                .item(option.getItem())
                .itemOption(option)
                .orderPrice(1100)
                .discountRate(0)
                .quantity(2)
                .build();
        when(orderItemService.createOrderItems(any(), any())).thenReturn(List.of(orderItem));

        when(couponApi.applyCoupon(10L, 1L, 2200L))
                .thenThrow(new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE));

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setAddress(createAddressRequestDto());
        requestDto.setOrderItems(List.of(new OrderItemRequestDto()));
        requestDto.setMemberCouponId(10L);

        assertThatThrownBy(() -> orderService.createInstanceOrder(1L, requestDto))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.COUPON_NOT_AVAILABLE);

        verify(couponApi).applyCoupon(10L, 1L, 2200L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성: 쿠폰 최소 주문 금액 미달이면 실패")
    void createInstanceOrder_couponMinOrderPriceNotMet() {
        Member member = createMember(1L);
        when(memberReadPort.getByIdWithLock(1L)).thenReturn(member);

        ItemOption option = createOption(createItem(), 100, 10);
        when(orderItemService.getItemOptionsWithLock(any())).thenReturn(List.of(option));

        OrderItem orderItem = OrderItem.builder()
                .item(option.getItem())
                .itemOption(option)
                .orderPrice(1100)
                .discountRate(0)
                .quantity(1)
                .build();
        when(orderItemService.createOrderItems(any(), any())).thenReturn(List.of(orderItem));

        when(couponApi.applyCoupon(500L, 1L, 1100L))
                .thenThrow(new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE));

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setAddress(createAddressRequestDto());
        requestDto.setOrderItems(List.of(new OrderItemRequestDto()));
        requestDto.setMemberCouponId(500L);

        assertThatThrownBy(() -> orderService.createInstanceOrder(1L, requestDto))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.COUPON_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("주문 취소: 주문 없음")
    void cancelOrder_notFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L, "변경"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("주문 취소: 이미 취소")
    void cancelOrder_alreadyCancelled() {
        Order order = Order.builder()
                .orderState(OrderState.CANCELLED)
                .member(createMember(1L))
                .orderItems(List.of())
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L, "변경"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("주문 취소: 성공")
    void cancelOrder_success() {
        Member member = createMember(1L);
        ItemOption option = createOption(createItem(), 100, 1);
        OrderItem orderItem = OrderItem.builder()
                .item(option.getItem())
                .itemOption(option)
                .orderPrice(1100)
                .discountRate(0)
                .quantity(1)
                .build();

        Order order = Order.builder()
                .orderState(OrderState.WAITING_FOR_PAY)
                .member(member)
                .usePoint(100)
                .memberCouponId(55L)
                .orderItems(List.of(orderItem))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderCancelResponseDto result = orderService.cancelOrder(member.getId(), 1L, "변경");

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getCancelReason()).isEqualTo("변경");
        assertThat(option.getStock()).isEqualTo(2);
        verify(memberPointPort).restorePoint(member.getId(), 100);
        verify(couponApi).restoreCoupon(55L);
    }

    @Test
    @DisplayName("주문 취소: 결제 완료 주문은 주문 API로 취소 불가")
    void cancelOrder_paidStateNotAllowed() {
        Member member = createMember(1L);
        Order order = Order.builder()
                .orderState(OrderState.PAID)
                .member(member)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(member.getId(), 1L, "변경"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_CANNOT_CANCEL);
    }

    @Test
    @DisplayName("주문 취소: 결제 요청 중(READY)은 취소 가능")
    void cancelOrder_paymentPendingReadyAllowed() {
        Member member = createMember(1L);

        Order order = Order.builder()
                .orderState(OrderState.PAYMENT_PENDING)
                .member(member)
                .orderItems(List.of())
                .build();
        ReflectionTestUtils.setField(order, "id", 1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderPaymentStatusPort.hasReadyPayment(1L)).thenReturn(true);

        OrderCancelResponseDto result = orderService.cancelOrder(member.getId(), 1L, "결제 취소");

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(order.getOrderState()).isEqualTo(OrderState.CANCELLED);
    }

    @Test
    @DisplayName("환불 보상: REFUND_PENDING 상태 주문도 취소/복구 가능")
    void cancelAndRestore_refundPendingAllowed() {
        Member member = createMember(1L);
        ItemOption option = createOption(createItem(), 100, 1);
        OrderItem orderItem = OrderItem.builder()
                .item(option.getItem())
                .itemOption(option)
                .orderPrice(1100)
                .discountRate(0)
                .quantity(1)
                .build();

        Order order = Order.builder()
                .orderState(OrderState.REFUND_PENDING)
                .member(member)
                .usePoint(50)
                .orderItems(List.of(orderItem))
                .build();

        orderService.cancelAndRestore(order);

        assertThat(order.getOrderState()).isEqualTo(OrderState.CANCELLED);
        assertThat(option.getStock()).isEqualTo(2);
        verify(memberPointPort).restorePoint(member.getId(), 50);
    }

    @Test
    @DisplayName("주문 자동 취소: 결제 대기 주문은 만료 시 취소 및 재고 복구")
    void autoCancelStaleUnpaidOrder_waitingForPay() {
        ItemOption option = createOption(createItem(), 100, 1);
        OrderItem orderItem = OrderItem.builder()
                .item(option.getItem())
                .itemOption(option)
                .orderPrice(1100)
                .discountRate(0)
                .quantity(1)
                .build();
        Order order = Order.builder()
                .orderState(OrderState.WAITING_FOR_PAY)
                .member(createMember(1L))
                .orderDate(LocalDateTime.now().minusHours(1))
                .orderItems(List.of(orderItem))
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        boolean cancelled = orderService.autoCancelStaleUnpaidOrder(1L, LocalDateTime.now().minusMinutes(30));

        assertThat(cancelled).isTrue();
        assertThat(order.getOrderState()).isEqualTo(OrderState.CANCELLED);
        assertThat(option.getStock()).isEqualTo(2);
        verify(orderPaymentStatusPort, never()).hasReadyPayment(anyLong());
    }

    @Test
    @DisplayName("주문 자동 취소: 결제 요청 상태(READY) 주문은 만료 시 취소")
    void autoCancelStaleUnpaidOrder_paymentPendingReady() {
        Order order = Order.builder()
                .orderState(OrderState.PAYMENT_PENDING)
                .member(createMember(1L))
                .orderDate(LocalDateTime.now().minusHours(1))
                .orderItems(List.of())
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderPaymentStatusPort.hasReadyPayment(1L)).thenReturn(true);

        boolean cancelled = orderService.autoCancelStaleUnpaidOrder(1L, LocalDateTime.now().minusMinutes(30));

        assertThat(cancelled).isTrue();
        assertThat(order.getOrderState()).isEqualTo(OrderState.CANCELLED);
    }

    @Test
    @DisplayName("주문 자동 취소: 결제 요청 상태에서 READY가 아니면 취소하지 않음")
    void autoCancelStaleUnpaidOrder_paymentPendingNotReady() {
        Order order = Order.builder()
                .orderState(OrderState.PAYMENT_PENDING)
                .member(createMember(1L))
                .orderDate(LocalDateTime.now().minusHours(1))
                .orderItems(List.of())
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderPaymentStatusPort.hasReadyPayment(1L)).thenReturn(false);

        boolean cancelled = orderService.autoCancelStaleUnpaidOrder(1L, LocalDateTime.now().minusMinutes(30));

        assertThat(cancelled).isFalse();
        assertThat(order.getOrderState()).isEqualTo(OrderState.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("주문 자동 취소: 만료 시간이 지나지 않았으면 취소하지 않음")
    void autoCancelStaleUnpaidOrder_notExpired() {
        Order order = Order.builder()
                .orderState(OrderState.WAITING_FOR_PAY)
                .member(createMember(1L))
                .orderDate(LocalDateTime.now().minusMinutes(5))
                .orderItems(List.of())
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        boolean cancelled = orderService.autoCancelStaleUnpaidOrder(1L, LocalDateTime.now().minusMinutes(30));

        assertThat(cancelled).isFalse();
        assertThat(order.getOrderState()).isEqualTo(OrderState.WAITING_FOR_PAY);
    }

    @Test
    @DisplayName("주문 취소: 주문 상태가 환불 진행 중(REFUND_PENDING)이면 취소 불가")
    void cancelOrder_refundPendingNotAllowed() {
        Member member = createMember(1L);
        Order order = Order.builder()
                .orderState(OrderState.REFUND_PENDING)
                .member(member)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(member.getId(), 1L, "환불 중 취소"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_CANNOT_CANCEL);
    }

    @Test
    @DisplayName("주문 요약 조회")
    void orderSummary() {
        Member member = createMember(1L);
        OrderResponseDto responseDto = new OrderResponseDto();
        when(orderRepository.findByOrderIdWithItemsAndMemberId(1L, member.getId()))
                .thenReturn(Optional.of(responseDto));

        OrderResponseDto result = orderService.orderSummary(member.getId(), 1L);

        assertThat(result).isSameAs(responseDto);
    }

    @Test
    @DisplayName("회원 주문 목록 조회")
    void memberOrders() {
        when(orderRepository.findOrderListById(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(new OrderResponseDto()), PageRequest.of(0, 20), 1));

        assertThat(orderService.memberOrders(1L, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("전체 주문 목록 조회")
    void allOrderList() {
        when(orderRepository.findAllOrders(any()))
                .thenReturn(new PageImpl<>(List.of(new OrderResponseDto()), PageRequest.of(0, 20), 1));

        assertThat(orderService.allOrderList(PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("구매 확정: 배송 완료")
    void purchaseConfirmation_delivered() {
        Member member = createMember(1L);
        Order order = Order.builder()
                .orderState(OrderState.DELIVERED)
                .member(member)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.purchaseConfirmation(member.getId(), 1L);

        assertThat(order.getOrderState()).isEqualTo(OrderState.COMPLETED);
    }

    @Test
    @DisplayName("구매 확정: 배송 미완료")
    void purchaseConfirmation_notDelivered() {
        Member member = createMember(1L);
        Order order = Order.builder()
                .orderState(OrderState.SHIPPING)
                .member(member)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.purchaseConfirmation(member.getId(), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_CANNOT_CONFIRM);
        assertThat(order.getOrderState()).isEqualTo(OrderState.SHIPPING);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경: PAID -> PREPARING_FOR_SHIPMENT")
    void updateOrderStateByAdmin_paidToPreparing() {
        Order order = Order.builder()
                .orderState(OrderState.PAID)
                .member(createMember(1L))
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.updateOrderStateByAdmin(1L, OrderState.PREPARING_FOR_SHIPMENT);

        assertThat(order.getOrderState()).isEqualTo(OrderState.PREPARING_FOR_SHIPMENT);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경: 허용되지 않은 전이는 실패")
    void updateOrderStateByAdmin_invalidTransition() {
        Order order = Order.builder()
                .orderState(OrderState.PAID)
                .member(createMember(1L))
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStateByAdmin(1L, OrderState.DELIVERED))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_INVALID_STATE_TRANSITION);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경: 환불 요청 접수 중이면 배송 상태 변경 불가")
    void updateOrderStateByAdmin_requestedRefund_shouldFail() {
        Order order = Order.builder()
                .orderState(OrderState.PAID)
                .refundRequestStatus(OrderRefundRequestStatus.REQUESTED)
                .member(createMember(1L))
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStateByAdmin(1L, OrderState.PREPARING_FOR_SHIPMENT))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_REFUND_REQUEST_IN_PROGRESS);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경: 환불 요청 승인 중이면 배송 상태 변경 불가")
    void updateOrderStateByAdmin_approvedRefund_shouldFail() {
        Order order = Order.builder()
                .orderState(OrderState.PAID)
                .refundRequestStatus(OrderRefundRequestStatus.APPROVED)
                .member(createMember(1L))
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStateByAdmin(1L, OrderState.PREPARING_FOR_SHIPMENT))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_REFUND_REQUEST_IN_PROGRESS);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경: 환불 요청 반려 후에는 배송 상태 변경 가능")
    void updateOrderStateByAdmin_rejectedRefund_shouldAllow() {
        Order order = Order.builder()
                .orderState(OrderState.PAID)
                .refundRequestStatus(OrderRefundRequestStatus.REJECTED)
                .member(createMember(1L))
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.updateOrderStateByAdmin(1L, OrderState.PREPARING_FOR_SHIPMENT);

        assertThat(order.getOrderState()).isEqualTo(OrderState.PREPARING_FOR_SHIPMENT);
    }

    @Test
    @DisplayName("환불 요청: 결제 완료 주문은 요청 성공")
    void requestRefundRequest_paidOrder_success() {
        Member member = createMember(1L);
        Order order = Order.builder()
                .orderState(OrderState.PAID)
                .member(member)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.requestRefundRequest(member.getId(), 1L, "단순 변심");

        assertThat(order.getRefundRequestStatus()).isEqualTo(OrderRefundRequestStatus.REQUESTED);
        assertThat(order.getRefundRequestReason()).isEqualTo("단순 변심");
        assertThat(order.getRefundRequestedAt()).isNotNull();
    }

    @Test
    @DisplayName("환불 요청: 결제 완료 외 상태는 요청 불가")
    void requestRefundRequest_notPaidOrder_fail() {
        Member member = createMember(1L);
        Order order = Order.builder()
                .orderState(OrderState.SHIPPING)
                .member(member)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.requestRefundRequest(member.getId(), 1L, "배송 중 요청"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_REFUND_REQUEST_NOT_ALLOWED);
    }

    @Test
    @DisplayName("환불 요청: 이미 접수 중인 요청이면 실패")
    void requestRefundRequest_alreadyRequested_fail() {
        Member member = createMember(1L);
        Order order = Order.builder()
                .orderState(OrderState.PAID)
                .refundRequestStatus(OrderRefundRequestStatus.REQUESTED)
                .member(member)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.requestRefundRequest(member.getId(), 1L, "중복 요청"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_REFUND_REQUEST_ALREADY_REQUESTED);
    }

    @Test
    @DisplayName("관리자 환불 요청 처리: REQUESTED -> APPROVED")
    void processRefundRequestByAdmin_requestedToApproved() {
        Order order = Order.builder()
                .orderState(OrderState.PAID)
                .refundRequestStatus(OrderRefundRequestStatus.REQUESTED)
                .member(createMember(1L))
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.processRefundRequestByAdmin(1L, OrderRefundRequestStatus.APPROVED, "환불 가능");

        assertThat(order.getRefundRequestStatus()).isEqualTo(OrderRefundRequestStatus.APPROVED);
        assertThat(order.getRefundProcessedReason()).isEqualTo("환불 가능");
        assertThat(order.getRefundProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("관리자 환불 요청 처리: APPROVED -> REFUNDED는 실패")
    void processRefundRequestByAdmin_approvedToRefunded_fail() {
        assertThatThrownBy(() -> orderService.processRefundRequestByAdmin(1L, OrderRefundRequestStatus.REFUNDED, "환불 완료"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_REFUND_REQUEST_INVALID_STATE_TRANSITION);
        verify(orderRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("관리자 환불 요청 처리: 접수되지 않은 요청은 실패")
    void processRefundRequestByAdmin_notRequested_fail() {
        Order order = Order.builder()
                .orderState(OrderState.PAID)
                .member(createMember(1L))
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.processRefundRequestByAdmin(1L, OrderRefundRequestStatus.APPROVED, "승인"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_REFUND_REQUEST_NOT_REQUESTED);
    }

    @Test
    @DisplayName("관리자 환불 요청 처리: REQUESTED -> REFUNDED는 실패")
    void processRefundRequestByAdmin_invalidTransition_fail() {
        assertThatThrownBy(() -> orderService.processRefundRequestByAdmin(1L, OrderRefundRequestStatus.REFUNDED, "바로 환불"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_REFUND_REQUEST_INVALID_STATE_TRANSITION);
        verify(orderRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("관리자 환불 요청 처리: REJECTED는 처리 사유가 필수")
    void processRefundRequestByAdmin_rejectedWithoutReason_fail() {
        assertThatThrownBy(() -> orderService.processRefundRequestByAdmin(1L, OrderRefundRequestStatus.REJECTED, " "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_REFUND_REQUEST_INVALID_STATE_TRANSITION);
        verify(orderRepository, never()).findById(anyLong());
    }

    private Member createMember(Long id) {
        Member member = Member.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .memberName("사용자" + id)
                .memberPwd("pw")
                .tel("01012341234")
                .build();
        ReflectionTestUtils.setField(member, "memberState", MemberState.ACTIVE);
        ReflectionTestUtils.setField(member, "role", Role.MEMBER);
        ReflectionTestUtils.setField(member, "lastPasswordChange", LocalDateTime.now());
        ReflectionTestUtils.setField(member, "point", 0);
        return member;
    }

    private AddressRequestDto createAddressRequestDto() {
        AddressRequestDto dto = new AddressRequestDto();
        dto.setCity("서울");
        dto.setStreet("강남");
        dto.setZipCode("12345");
        return dto;
    }

    private Item createItem() {
        return Item.builder()
                .itemName("커피")
                .price(1000)
                .deliveryFee(0)
                .origin("브라질")
                .itemLimit(10)
                .seller(createMember(2L))
                .build();
    }

    private ItemOption createOption(Item item, int additionalPrice, int stock) {
        return ItemOption.builder()
                .item(item)
                .additionalPrice(additionalPrice)
                .stock(stock)
                .build();
    }
}
