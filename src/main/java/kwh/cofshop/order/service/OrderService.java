package kwh.cofshop.order.service;

import jakarta.persistence.EntityNotFoundException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
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
import kwh.cofshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final MemberRepository memberRepository;

    private final OrderItemService orderItemService;

    @Transactional
    public OrderResponseDto createOrder(Long memberId, Address address, List<OrderItemRequestDto> itemRequestDtoList) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        // 옵션 조회
        List<ItemOption> itemOptions = orderItemService.getItemOptionsWithLock(itemRequestDtoList);

        // 주문 항목 생성
        List<OrderItem> orderItems = orderItemService.createOrderItems(itemRequestDtoList, itemOptions);

        // 주문 생성
        Order order = Order.createOrder(member, address, orderItems);

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


}
