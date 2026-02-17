package kwh.cofshop.order.repository.custom;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.QItem;
import kwh.cofshop.item.domain.QItemOption;
import kwh.cofshop.order.domain.QOrder;
import kwh.cofshop.order.domain.QOrderItem;
import kwh.cofshop.order.dto.response.AddressResponseDto;
import kwh.cofshop.order.dto.response.OrderItemResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;


    @Override
    public Page<OrderResponseDto> findOrderListById(Long id, Pageable pageable) {
        QOrder order = QOrder.order;

        long total = countByMemberId(id);
        if (total == 0L) {
            return new PageImpl<>(List.of(), pageable, 0L);
        }

        List<Long> orderIds = queryFactory
                .select(order.id)
                .from(order)
                .where(order.member.id.eq(id))
                .orderBy(order.createDate.desc(), order.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<OrderResponseDto> content = fetchOrderResponses(orderIds);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Optional<OrderResponseDto> findByOrderIdWithItemsAndMemberId(Long orderId, Long memberId) {
        QOrder order = QOrder.order;

        Long foundOrderId = queryFactory
                .select(order.id)
                .from(order)
                .where(order.id.eq(orderId), order.member.id.eq(memberId))
                .fetchOne();

        if (foundOrderId == null) {
            return Optional.empty();
        }
        return fetchOrderResponses(List.of(foundOrderId)).stream().findFirst();
    }

    @Override
    public Page<OrderResponseDto> findAllOrders(Pageable pageable) {
        QOrder order = QOrder.order;

        long total = countAllOrders();
        if (total == 0L) {
            return new PageImpl<>(List.of(), pageable, 0L);
        }

        List<Long> orderIds = queryFactory
                .select(order.id)
                .from(order)
                .orderBy(order.createDate.desc(), order.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<OrderResponseDto> content = fetchOrderResponses(orderIds);

        return new PageImpl<>(content, pageable, total);
    }

    private long countByMemberId(Long memberId) {
        QOrder order = QOrder.order;
        return Optional.ofNullable(
                queryFactory
                        .select(order.count())
                        .from(order)
                        .where(order.member.id.eq(memberId))
                        .fetchOne()
        ).orElse(0L);
    }

    private long countAllOrders() {
        QOrder order = QOrder.order;
        return Optional.ofNullable(
                queryFactory
                        .select(order.count())
                        .from(order)
                        .fetchOne()
        ).orElse(0L);
    }

    private List<OrderResponseDto> fetchOrderResponses(List<Long> orderIds) {
        Map<Long, OrderResponseDto> orderById = fetchOrderHeaders(orderIds);
        appendOrderItems(orderById, orderIds);

        return orderIds.stream()
                .map(orderById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<Long, OrderResponseDto> fetchOrderHeaders(List<Long> orderIds) {
        QOrder order = QOrder.order;

        List<Tuple> headerRows = queryFactory
                .select(
                        order.id,
                        order.orderState,
                        order.deliveryRequest,
                        order.deliveryFee,
                        order.totalPrice,
                        order.discountFromCoupon,
                        order.usePoint,
                        order.finalPrice,
                        order.refundRequestStatus,
                        order.refundRequestReason,
                        order.refundRequestedAt,
                        order.refundProcessedAt,
                        order.refundProcessedReason,
                        order.address.city,
                        order.address.street,
                        order.address.zipCode
                )
                .from(order)
                .where(order.id.in(orderIds))
                .fetch();

        Map<Long, OrderResponseDto> orderById = new LinkedHashMap<>();
        for (Tuple row : headerRows) {
            Long orderId = row.get(order.id);
            if (orderId == null) {
                continue;
            }

            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setOrderId(orderId);
            responseDto.setOrderState(row.get(order.orderState));
            responseDto.setDeliveryRequest(row.get(order.deliveryRequest));
            responseDto.setDeliveryFee(row.get(order.deliveryFee));
            responseDto.setTotalPrice(row.get(order.totalPrice));
            responseDto.setDiscountFromCoupon(row.get(order.discountFromCoupon));
            responseDto.setUsePoint(row.get(order.usePoint));
            responseDto.setFinalPrice(row.get(order.finalPrice));
            responseDto.setRefundRequestStatus(row.get(order.refundRequestStatus));
            responseDto.setRefundRequestReason(row.get(order.refundRequestReason));
            responseDto.setRefundRequestedAt(row.get(order.refundRequestedAt));
            responseDto.setRefundProcessedAt(row.get(order.refundProcessedAt));
            responseDto.setRefundProcessedReason(row.get(order.refundProcessedReason));
            responseDto.setOrderItems(new ArrayList<>());

            AddressResponseDto addressResponseDto = new AddressResponseDto();
            addressResponseDto.setCity(row.get(order.address.city));
            addressResponseDto.setStreet(row.get(order.address.street));
            addressResponseDto.setZipCode(row.get(order.address.zipCode));
            responseDto.setAddress(addressResponseDto);

            orderById.put(orderId, responseDto);
        }
        return orderById;
    }

    private void appendOrderItems(Map<Long, OrderResponseDto> orderById, List<Long> orderIds) {
        if (orderById.isEmpty()) {
            return;
        }

        QOrderItem orderItem = QOrderItem.orderItem;
        QItem item = QItem.item;
        QItemOption itemOption = QItemOption.itemOption;

        List<Tuple> itemRows = queryFactory
                .select(
                        orderItem.order.id,
                        item.itemName,
                        orderItem.orderPrice,
                        itemOption.additionalPrice,
                        orderItem.discountRate,
                        item.origin,
                        orderItem.quantity
                )
                .from(orderItem)
                .join(orderItem.item, item)
                .join(orderItem.itemOption, itemOption)
                .where(orderItem.order.id.in(orderIds))
                .orderBy(orderItem.order.id.asc(), orderItem.id.asc())
                .fetch();

        for (Tuple row : itemRows) {
            Long orderId = row.get(orderItem.order.id);
            OrderResponseDto responseDto = orderById.get(orderId);
            if (responseDto == null) {
                continue;
            }

            OrderItemResponseDto orderItemResponseDto = new OrderItemResponseDto();
            orderItemResponseDto.setItemName(row.get(item.itemName));
            orderItemResponseDto.setPrice(row.get(orderItem.orderPrice));
            orderItemResponseDto.setAdditionalPrice(row.get(itemOption.additionalPrice));
            orderItemResponseDto.setDiscountRate(row.get(orderItem.discountRate));
            orderItemResponseDto.setOrigin(row.get(item.origin));
            orderItemResponseDto.setQuantity(row.get(orderItem.quantity));

            responseDto.getOrderItems().add(orderItemResponseDto);
        }
    }
}
