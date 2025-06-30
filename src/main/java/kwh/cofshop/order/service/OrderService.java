package kwh.cofshop.order.service;

import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.service.MemberCouponService;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.member.service.MemberService;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.dto.request.OrderCancelRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.response.OrderCancelResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.mapper.OrderMapper;
import kwh.cofshop.order.policy.DeliveryFeePolicy;
import kwh.cofshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {


    // Order 관련
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderItemService orderItemService;
    private final DeliveryFeePolicy deliveryFeePolicy;

    // Member 관련 서비스
    private final MemberService memberService;
    private final MemberCouponService memberCouponService;

    // 바로구매 로직
    @Transactional
    public OrderResponseDto createInstanceOrder(Long memberId, OrderRequestDto orderRequestDto) {
        Member member = memberService.getMember(memberId);

        // 옵션 조회
        List<ItemOption> itemOptions = orderItemService.getItemOptionsWithLock(orderRequestDto.getOrderItemRequestDtoList());

        // 주문 항목 생성
        List<OrderItem> orderItems = orderItemService.createOrderItems(orderRequestDto.getOrderItemRequestDtoList(), itemOptions);

        // 주문 생성
        Order order = Order.createOrder(member, orderRequestDto.getAddress(), orderItems);

        // 쿠폰 할인 금액
        long priceAfterCouponDiscount = applyCoupon(order, orderRequestDto, memberId);

        // 포인트 할인 금액
        applyPoint(order, orderRequestDto, member, priceAfterCouponDiscount);

        // 배송비
        int deliveryFee = deliveryFeePolicy.calculate(itemOptions);

        // 최종 금액 계산
        order.finalizePrice(priceAfterCouponDiscount, orderRequestDto.getUsePoint(), deliveryFee);

        // 저장 및 반환
        return orderMapper.toResponseDto(orderRepository.save(order));
    } // 주문 생성을 위한 기틀


    @Transactional // 주문 취소
    public OrderCancelResponseDto cancelOrder(OrderCancelRequestDto orderCancelRequestDto) {
        // 1. 주문 찾기
        Order order = orderRepository.findById(orderCancelRequestDto.getOrderId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND)); // 주문 정보

        // 2. 주문 상태 변경 (취소 처리)
        order.cancel();

        // 3. 사용 포인트 및 쿠폰 복구 처리
        restoreMemberPointAndCoupon(order);

        OrderCancelResponseDto orderCancelResponseDto = new OrderCancelResponseDto();
        orderCancelResponseDto.setOrderId(orderCancelRequestDto.getOrderId());
        orderCancelResponseDto.setCancelReason(orderCancelRequestDto.getCancelReason());

        return orderCancelResponseDto;
    }

    private void restoreMemberPointAndCoupon(Order order) {
        if (order.getUsePoint() > 0) {
            memberService.restorePoint(order.getMember().getId(), order.getUsePoint());
        }
        if (order.getMemberCoupon() != null) {
            memberCouponService.restoreCoupon(order.getMemberCoupon().getId());
        }
    }

    // 포인트, 쿠폰 복구
    public void restorePointAndCoupon(Order order) {
        order.getOrderItems().forEach(OrderItem::restoreStock);

        if (order.getUsePoint() > 0) {
            order.getMember().restorePoint(order.getUsePoint());
        }

        if (order.getMemberCoupon() != null) {
            order.getMemberCoupon().restoreCouponStatus();
        }
    }

    // 하나의 주문 정보
    @Transactional(readOnly = true)
    public OrderResponseDto orderSummary(Long orderId) {
        return orderRepository.findByOrderIdWithItems(orderId);
    }

    // 멤버의 주문 목록
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> memberOrders(Long id, Pageable pageable){
        return orderRepository.findOrderListById(id, pageable);
    }

    // 모든 주문 리스트 보기
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> allOrderList(Pageable pageable){
        return orderRepository.findAllOrders(pageable);
    }

    // 구매 확정(소비자)
    @Transactional
    public void PurchaseConfirmation(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));
        if (order.getOrderState() == OrderState.DELIVERED){ // 배송 완료 시
            order.changeOrderState(OrderState.COMPLETED); // 구매 확정 가능 - 이 시점에서 반품 불가능함
        }
    }


    // 쿠폰 할인 적용
    private long applyCoupon(Order order, OrderRequestDto dto, Long memberId) {
        Long couponDiscountValue = 0L;

        if (dto.getMemberCouponId() != null) {
            MemberCoupon memberCoupon = memberCouponService.findValidCoupon(dto.getMemberCouponId(), memberId);
            couponDiscountValue = memberCoupon.getCoupon().calculateDiscount(order.getTotalPrice());
            memberCoupon.useCoupon();
            order.addUseCoupon(memberCoupon, couponDiscountValue);
        }

        return order.getTotalPrice() - couponDiscountValue;
    }

    // 포인트 적용
    private void applyPoint(Order order, OrderRequestDto dto, Member member, long priceAfterCoupon) {
        int usePoint = dto.getUsePoint();

        if (usePoint > 0 && usePoint <= priceAfterCoupon) {
            member.usePoint(usePoint);
            order.addUsePoint(usePoint);
        }
    }
}
