package kwh.cofshop.order.service;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.factory.CouponDiscountPolicyFactory;
import kwh.cofshop.coupon.policy.discount.CouponDiscountPolicy;
import kwh.cofshop.coupon.service.MemberCouponService;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.member.service.MemberService;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.dto.request.OrderCancelRequestDto;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.response.OrderCancelResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.mapper.OrderMapper;
import kwh.cofshop.order.policy.DeliveryFeePolicy;
import kwh.cofshop.order.repository.OrderRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when(memberService.getMember(1L)).thenReturn(member);

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
        requestDto.setAddress(Address.builder().city("서울").street("강남").zipCode("12345").build());
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
        when(memberService.getMember(1L)).thenReturn(member);

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
        MemberCoupon memberCoupon = MemberCoupon.builder().coupon(coupon).state(CouponState.AVAILABLE).build();
        when(memberCouponService.findValidCoupon(10L, 1L)).thenReturn(memberCoupon);

        CouponDiscountPolicy policy = org.mockito.Mockito.mock(CouponDiscountPolicy.class);
        when(couponDiscountPolicyFactory.getPolicy(CouponType.FIXED)).thenReturn(policy);
        when(policy.calculateDiscount(2200L)).thenReturn(500L);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponseDto(any(Order.class))).thenReturn(new OrderResponseDto());

        OrderRequestDto requestDto = new OrderRequestDto();
        requestDto.setAddress(Address.builder().city("서울").street("강남").zipCode("12345").build());
        requestDto.setOrderItemRequestDtoList(List.of(new OrderItemRequestDto()));
        requestDto.setMemberCouponId(10L);
        requestDto.setUsePoint(100);

        OrderResponseDto result = orderService.createInstanceOrder(1L, requestDto);

        assertThat(result).isNotNull();
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getFinalPrice()).isEqualTo(2200 - 500 - 100 + 2500);
        assertThat(saved.getMemberCoupon()).isSameAs(memberCoupon);
    }

    @Test
    @DisplayName("주문 취소: 주문 없음")
    void cancelOrder_notFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        OrderCancelRequestDto requestDto = new OrderCancelRequestDto();
        requestDto.setOrderId(1L);

        assertThatThrownBy(() -> orderService.cancelOrder(requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("주문 취소: 이미 취소")
    void cancelOrder_alreadyCancelled() {
        Order order = Order.builder().orderState(OrderState.CANCELLED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderCancelRequestDto requestDto = new OrderCancelRequestDto();
        requestDto.setOrderId(1L);

        assertThatThrownBy(() -> orderService.cancelOrder(requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("주문 취소: 성공")
    void cancelOrder_success() {
        Member member = createMember(1L);
        MemberCoupon memberCoupon = MemberCoupon.builder().build();
        ReflectionTestUtils.setField(memberCoupon, "id", 55L);

        Order order = Order.builder()
                .orderState(OrderState.WAITING_FOR_PAY)
                .member(member)
                .usePoint(100)
                .memberCoupon(memberCoupon)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderCancelRequestDto requestDto = new OrderCancelRequestDto();
        requestDto.setOrderId(1L);
        requestDto.setCancelReason("변경");

        OrderCancelResponseDto result = orderService.cancelOrder(requestDto);

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getCancelReason()).isEqualTo("변경");
        verify(memberService).restorePoint(member.getId(), 100);
        verify(memberCouponService).restoreCoupon(55L);
    }

    @Test
    @DisplayName("주문 요약 조회")
    void orderSummary() {
        OrderResponseDto responseDto = new OrderResponseDto();
        when(orderRepository.findByOrderIdWithItems(1L)).thenReturn(responseDto);

        OrderResponseDto result = orderService.orderSummary(1L);

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
        Order order = Order.builder().orderState(OrderState.DELIVERED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.PurchaseConfirmation(1L);

        assertThat(order.getOrderState()).isEqualTo(OrderState.COMPLETED);
    }

    @Test
    @DisplayName("구매 확정: 배송 미완료")
    void purchaseConfirmation_notDelivered() {
        Order order = Order.builder().orderState(OrderState.SHIPPING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.PurchaseConfirmation(1L);

        assertThat(order.getOrderState()).isEqualTo(OrderState.SHIPPING);
    }

    @Test
    @DisplayName("포인트/쿠폰 복구")
    void restorePointAndCoupon() {
        Member member = createMember(1L);
        ReflectionTestUtils.setField(member, "point", 0);

        MemberCoupon memberCoupon = MemberCoupon.builder().state(CouponState.USED).build();

        ItemOption option = createOption(createItem(), 100, 1);
        OrderItem orderItem = OrderItem.builder()
                .item(option.getItem())
                .itemOption(option)
                .orderPrice(1100)
                .discountRate(0)
                .quantity(1)
                .build();

        Order order = Order.builder()
                .member(member)
                .usePoint(100)
                .memberCoupon(memberCoupon)
                .orderItems(List.of(orderItem))
                .build();

        orderService.restorePointAndCoupon(order);

        assertThat(option.getStock()).isEqualTo(2);
        assertThat(member.getPoint()).isEqualTo(100);
        assertThat(memberCoupon.getState()).isEqualTo(CouponState.AVAILABLE);
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
