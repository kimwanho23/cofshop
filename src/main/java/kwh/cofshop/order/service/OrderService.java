package kwh.cofshop.order.service;

import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.factory.CouponDiscountPolicyFactory;
import kwh.cofshop.coupon.policy.discount.CouponDiscountPolicy;
import kwh.cofshop.coupon.service.MemberCouponService;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.ForbiddenErrorCode;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.service.MemberService;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.domain.OrderState;
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

    // 쿠폰 할인 정책
    private final CouponDiscountPolicyFactory couponDiscountPolicyFactory;

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

        // 쿠폰 할인 적용 금액
        long priceAfterCouponDiscount = applyCoupon(order, orderRequestDto, memberId);

        // 포인트 할인 금액
        int usedPoint = applyPoint(order, orderRequestDto, member, priceAfterCouponDiscount);

        // 배송비
        int deliveryFee = deliveryFeePolicy.calculate(itemOptions);

        // 최종 금액 계산
        order.finalizePrice(priceAfterCouponDiscount, usedPoint, deliveryFee);

        // 저장 및 반환
        return orderMapper.toResponseDto(orderRepository.save(order));
    } // 주문 생성을 위한 기틀


    @Transactional // 주문 취소
    public OrderCancelResponseDto cancelOrder(Long memberId, Long orderId, String cancelReason) {
        Order order = getOrderForMember(memberId, orderId);

        order.cancel();
        restoreOrderCompensation(order);

        OrderCancelResponseDto orderCancelResponseDto = new OrderCancelResponseDto();
        orderCancelResponseDto.setOrderId(orderId);
        orderCancelResponseDto.setCancelReason(cancelReason);

        return orderCancelResponseDto;
    }

    private void restoreOrderCompensation(Order order) {
        restoreStock(order);
        restoreMemberBenefits(order);
    }

    private void restoreStock(Order order) {
        if (order.getOrderItems() == null) {
            return;
        }
        order.getOrderItems().forEach(OrderItem::restoreStock);
    }

    private void restoreMemberBenefits(Order order) {
        if (order.getUsePoint() != null && order.getUsePoint() > 0) {
            memberService.restorePoint(order.getMember().getId(), order.getUsePoint());
        }
        if (order.getMemberCoupon() != null) {
            memberCouponService.restoreCoupon(order.getMemberCoupon().getId());
        }
    }

    // 하나의 주문 정보
    @Transactional(readOnly = true)
    public OrderResponseDto orderSummary(Long memberId, Long orderId) {
        return orderRepository.findByOrderIdWithItemsAndMemberId(orderId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));
    }

    // 멤버의 주문 목록
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> memberOrders(Long id, Pageable pageable) {
        return orderRepository.findOrderListById(id, pageable);
    }

    // 모든 주문 리스트 보기
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> allOrderList(Pageable pageable) {
        return orderRepository.findAllOrders(pageable);
    }

    // 구매 확정(소비자)
    @Transactional
    public void purchaseConfirmation(Long memberId, Long orderId) {
        Order order = getOrderForMember(memberId, orderId);
        if (order.getOrderState() == OrderState.DELIVERED) { // 배송 완료 시
            order.changeOrderState(OrderState.COMPLETED); // 구매 확정 가능 - 이 시점에서 반품 불가능함
        }
    }


    // 쿠폰 할인 적용
    private long applyCoupon(Order order, OrderRequestDto dto, Long memberId) {
        if (dto.getMemberCouponId() == null) {
            return order.getTotalPrice();
        }

        MemberCoupon memberCoupon = memberCouponService.findValidCoupon(dto.getMemberCouponId(), memberId);
        CouponDiscountPolicy policy = couponDiscountPolicyFactory.getPolicy(memberCoupon.getCoupon().getType());
        Long discount = policy.calculateDiscount(order.getTotalPrice());

        memberCoupon.useCoupon();
        order.addUseCoupon(memberCoupon, discount);

        return order.getTotalPrice() - discount;
    }

    // 포인트 적용
    private int applyPoint(Order order, OrderRequestDto dto, Member member, long priceAfterCoupon) {
        Integer requestedPoint = dto.getUsePoint();
        if (requestedPoint == null || requestedPoint <= 0) {
            return 0;
        }

        int availablePoint = member.getPoint() == null ? 0 : member.getPoint();
        int usablePoint = (int) Math.min(Math.min(requestedPoint, availablePoint), priceAfterCoupon);

        if (usablePoint <= 0) {
            return 0;
        }

        member.usePoint(usablePoint);
        order.addUsePoint(usablePoint);
        return usablePoint;
    }

    private Order getOrderForMember(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));

        if (!order.getMember().getId().equals(memberId)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.MEMBER_UNAUTHORIZED);
        }

        return order;
    }
}
