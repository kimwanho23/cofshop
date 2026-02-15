package kwh.cofshop.order.service;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.application.factory.CouponDiscountPolicyFactory;
import kwh.cofshop.coupon.domain.policy.discount.CouponDiscountPolicy;
import kwh.cofshop.coupon.application.service.MemberCouponService;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.member.service.MemberService;
import kwh.cofshop.order.domain.Order;
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
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
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
    private MemberService memberService;

    @Mock
    private MemberCouponService memberCouponService;

    @Mock
    private CouponDiscountPolicyFactory couponDiscountPolicyFactory;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성: 쿠폰/포인트 없음")
    void createInstanceOrder_noCouponNoPoint() {
        Member member = createMember(1L);
        when(memberService.getMemberWithLock(1L)).thenReturn(member);

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
        requestDto.setOrderItemRequestDtoList(List.of(new OrderItemRequestDto()));
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
        when(memberService.getMemberWithLock(1L)).thenReturn(member);

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

        Coupon coupon = Coupon.builder().type(CouponType.FIXED).state(CouponState.AVAILABLE).build();
        ReflectionTestUtils.setField(coupon, "id", 999L);
        MemberCoupon memberCoupon = MemberCoupon.builder().coupon(coupon).state(CouponState.AVAILABLE).build();
        ReflectionTestUtils.setField(memberCoupon, "id", 500L);
        when(memberCouponService.findValidCoupon(500L, 1L)).thenReturn(memberCoupon);

        CouponDiscountPolicy policy = org.mockito.Mockito.mock(CouponDiscountPolicy.class);
        when(couponDiscountPolicyFactory.getPolicy(CouponType.FIXED)).thenReturn(policy);
        when(policy.calculateDiscount(2200L, coupon)).thenReturn(500L);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponseDto(any(Order.class))).thenReturn(new OrderResponseDto());

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setAddress(createAddressRequestDto());
        requestDto.setOrderItemRequestDtoList(List.of(new OrderItemRequestDto()));
        requestDto.setMemberCouponId(500L);
        requestDto.setUsePoint(100);

        OrderResponseDto result = orderService.createInstanceOrder(1L, requestDto);

        assertThat(result).isNotNull();
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getFinalPrice()).isEqualTo(2200 - 500 - 100 + 2500);
        assertThat(saved.getMemberCoupon()).isSameAs(memberCoupon);
        verify(memberCouponService).findValidCoupon(500L, 1L);
    }

    @Test
    @DisplayName("주문 생성: couponId를 memberCouponId로 보내면 쿠폰 조회 실패")
    void createInstanceOrder_withCouponTemplateId_shouldFail() {
        Member member = createMember(1L);
        when(memberService.getMemberWithLock(1L)).thenReturn(member);

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

        when(memberCouponService.findValidCoupon(10L, 1L))
                .thenThrow(new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE));

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setAddress(createAddressRequestDto());
        requestDto.setOrderItemRequestDtoList(List.of(new OrderItemRequestDto()));
        requestDto.setMemberCouponId(10L);

        assertThatThrownBy(() -> orderService.createInstanceOrder(1L, requestDto))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.COUPON_NOT_AVAILABLE);

        verify(memberCouponService).findValidCoupon(10L, 1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성: 쿠폰 최소 주문 금액 미달이면 실패")
    void createInstanceOrder_couponMinOrderPriceNotMet() {
        Member member = createMember(1L);
        when(memberService.getMemberWithLock(1L)).thenReturn(member);

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

        Coupon coupon = Coupon.builder()
                .type(CouponType.FIXED)
                .state(CouponState.AVAILABLE)
                .minOrderPrice(5000)
                .build();
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .state(CouponState.AVAILABLE)
                .build();
        when(memberCouponService.findValidCoupon(500L, 1L)).thenReturn(memberCoupon);

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setAddress(createAddressRequestDto());
        requestDto.setOrderItemRequestDtoList(List.of(new OrderItemRequestDto()));
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
        MemberCoupon memberCoupon = MemberCoupon.builder().build();
        ReflectionTestUtils.setField(memberCoupon, "id", 55L);
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
                .memberCoupon(memberCoupon)
                .orderItems(List.of(orderItem))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderCancelResponseDto result = orderService.cancelOrder(member.getId(), 1L, "변경");

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getCancelReason()).isEqualTo("변경");
        assertThat(option.getStock()).isEqualTo(2);
        verify(memberService).restorePoint(member.getId(), 100);
        verify(memberCouponService).restoreCoupon(55L);
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
        PaymentEntity payment = org.mockito.Mockito.mock(PaymentEntity.class);
        when(payment.getStatus()).thenReturn(PaymentStatus.READY);

        Order order = Order.builder()
                .orderState(OrderState.PAYMENT_PENDING)
                .member(member)
                .orderItems(List.of())
                .payment(payment)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

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
        verify(memberService).restorePoint(member.getId(), 50);
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
