package kwh.cofshop.order.service;

import jakarta.persistence.EntityNotFoundException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
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

    private final ItemOptionRepository itemOptionRepository;

    @Transactional // 상품 주문
    public OrderResponseDto createOrder(OrderRequestDto orderRequestDto, Long id) {

        Member member = memberRepository.findById(id).orElseThrow();

        List<Long> optionIds = orderRequestDto.getOrderItemRequestDtoList()
                .stream()
                .map(OrderItemRequestDto::getOptionId)
                .toList();

        Map<Long, ItemOption> itemOptionMap = optionIds.stream()
                .map(optionId -> itemOptionRepository.findByItemOptionIdWithLock(optionId)
                        .orElseThrow(() -> new EntityNotFoundException("옵션을 찾을 수 없습니다.")))
                .collect(Collectors.toMap(ItemOption::getId, Function.identity()));


        List<OrderItem> orderItems = orderRequestDto.getOrderItemRequestDtoList().stream()
                .map(dto -> {
                    ItemOption itemOption = itemOptionMap.get(dto.getOptionId());
                    itemOption.removeStock(dto.getQuantity());
                    return OrderItem.createOrderItem(itemOption.getItem(), itemOption, dto.getQuantity());
                }).toList();

        // Order 생성
        Order order = Order.createOrder(
                member,
                orderRequestDto.getOrdererRequestDto().getAddress(),
                orderItems
        );

        // Order 저장 및 DTO 변환 후 반환
        Order saveOrder = orderRepository.save(order);
        return orderMapper.toResponseDto(saveOrder);
    }

    @Transactional // 주문 취소
    public OrderCancelResponseDto cancelOrder(OrderCancelRequestDto orderCancelRequestDto) {
        // 1. 주문 찾기
        Order order = orderRepository.findById(orderCancelRequestDto.getOrderId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND)); // 주문 정보

        // 2. 주문 상태 변경 (취소 처리)
        order.changeOrderState(OrderState.CANCELLED);

        // 3. 재고 복구

        for (OrderItem orderItem : order.getOrderItems()) {
            ItemOption itemOption = orderItem.getItemOption();
            itemOption.addStock(orderItem.getQuantity());
        }

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
