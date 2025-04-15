package kwh.cofshop.order.service;

import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.service.CouponService;
import kwh.cofshop.coupon.service.MemberCouponService;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final MemberRepository memberRepository;

    private final OrderItemService orderItemService;
    private final MemberCouponService memberCouponService;
    private final DeliveryFeePolicy deliveryFeePolicy;

    // 바로구매 로직
    @Transactional
    public OrderResponseDto createInstanceOrder(Long memberId, OrderRequestDto orderRequestDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        // 옵션 조회
        List<ItemOption> itemOptions = orderItemService.getItemOptionsWithLock(orderRequestDto.getOrderItemRequestDtoList());

        // 주문 항목 생성
        List<OrderItem> orderItems = orderItemService.createOrderItems(orderRequestDto.getOrderItemRequestDtoList(), itemOptions);

        // 주문 생성
        Order order = Order.createOrder(member, orderRequestDto.getAddress(), orderItems);

        int totalPrice = order.getTotalPrice(); // 현재의 총 금액


        //////// 쿠폰 할인 적용
        MemberCoupon memberCoupon = null; // 쿠폰
        int couponDiscountValue = 0; // 쿠폰 할인 가격

        if (orderRequestDto.getMemberCouponId() != null) {
            memberCoupon = memberCouponService.findValidCoupon(orderRequestDto.getMemberCouponId(), memberId); // 사용 가능한 쿠폰 등록
            couponDiscountValue = memberCoupon.getCoupon().calculateDiscount(order.getTotalPrice()); // 쿠폰으로 가격 계산
            memberCoupon.useCoupon(); // 쿠폰 사용
            order.addUseCoupon(memberCoupon, couponDiscountValue); // Order에 사용한 쿠폰 등록
        }
        int useAfterCouponValue = totalPrice - couponDiscountValue; // 쿠폰 사용 후 가격


        //////////포인트 사용
        int usePoint = orderRequestDto.getUsePoint(); // 포인트


        if (usePoint > 0 && usePoint <= useAfterCouponValue) {
            member.usePoint(usePoint);
            order.addUsePoint(usePoint);
        }

        //////////// 배송비 계산, 최종 금액 계산
        int deliveryFee = deliveryFeePolicy.calculate(itemOptions); // 배송비
        int finalPrice = useAfterCouponValue - usePoint + deliveryFee; // 최종 가격

        order.addDeliveryFee(deliveryFee);
        order.addFinalPrice(finalPrice);

        // 저장 및 반환
        return orderMapper.toResponseDto(orderRepository.save(order));
    }


    @Transactional // 주문 취소
    public OrderCancelResponseDto cancelOrder(OrderCancelRequestDto orderCancelRequestDto) {
        // 1. 주문 찾기
        Order order = orderRepository.findById(orderCancelRequestDto.getOrderId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND)); // 주문 정보

        // 2. 주문 상태 변경 (취소 처리)
        order.cancel();

        OrderCancelResponseDto orderCancelResponseDto = new OrderCancelResponseDto();
        orderCancelResponseDto.setOrderId(orderCancelRequestDto.getOrderId());
        orderCancelResponseDto.setCancelReason(orderCancelRequestDto.getCancelReason());

        return orderCancelResponseDto;
    }

    // 반품 요청

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
        if (order.getOrderState() == OrderState.SHIPPED){ // 배송 완료 시
            order.changeOrderState(OrderState.COMPLETED); // 구매 확정 가능
        }
    }
}
