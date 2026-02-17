package kwh.cofshop.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import kwh.cofshop.global.annotation.LoginMember;
import kwh.cofshop.order.dto.request.OrderCancelRequestDto;
import kwh.cofshop.order.dto.request.OrderRefundRequestProcessDto;
import kwh.cofshop.order.dto.request.OrderRefundRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.request.OrderStateUpdateRequestDto;
import kwh.cofshop.order.dto.response.OrderCancelResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@SecurityRequirement(name = "Bearer Authentication")
public class OrderController {

    private final OrderService orderService;

    //////////// @GET
    // 하나의 상품 주문 정보 조회
    @Operation(summary = "상품 주문 정보 조회", description = "하나의 상품의 주문정보를 조회합니다.")
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('MEMBER')")
    public OrderResponseDto getOrderInfo(
            @PathVariable Long orderId,
            @LoginMember Long memberId) {
        return orderService.orderSummary(memberId, orderId);
    }

    // 현재 자신의 전체 주문 조회
    @Operation(summary = "한 사람의 주문 전체 조회", description = "구매한 모든 상품의 주문정보를 조회합니다.")
    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public Page<OrderResponseDto> getMyOrders(
            @LoginMember Long memberId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return orderService.memberOrders(memberId, pageable);
    }


    // 모든 주문 조회
    @Operation(summary = "주문 전체 조회", description = "관리자 전용입니다, 전체 주문 정보를 조회합니다.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderResponseDto> getAllOrders(
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.allOrderList(pageable);
    }

    //////////// @POST
    // 주문 생성
    @Operation(summary = "주문 생성", description = "상품 주문 정보를 생성합니다.")
    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<OrderResponseDto> createOrder(
            @LoginMember Long memberId,
            @Valid @RequestBody OrderRequestDto requestDto) {

        OrderResponseDto responseDto = orderService.createInstanceOrder(memberId, requestDto);

        return ResponseEntity.created(URI.create("/api/orders/" + responseDto.getOrderId()))
                .body(responseDto);
    }

    @Operation(summary = "환불 요청", description = "회원이 결제 완료 주문에 대해 환불 요청을 접수합니다.")
    @PostMapping("/{orderId}/refund-request")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> requestRefundRequest(
            @PathVariable Long orderId,
            @LoginMember Long memberId,
            @Valid @RequestBody OrderRefundRequestDto requestDto) {
        orderService.requestRefundRequest(memberId, orderId, requestDto.getRefundRequestReason());
        return ResponseEntity.noContent().build();
    }


    //////////// @PUT, PATCH
    // 주문 취소
    @Operation(summary = "주문 취소", description = "상품의 주문을 취소합니다.")
    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('MEMBER')")
    public OrderCancelResponseDto cancelOrder(
            @PathVariable Long orderId,
            @LoginMember Long memberId,
            @Valid @RequestBody OrderCancelRequestDto dto) {

        return orderService.cancelOrder(memberId, orderId, dto.getCancelReason());
    }

    // 구매 확정
    @Operation(summary = "구매 확정", description = "배송 완료된 상품의 구매를 확정합니다.")
    @PatchMapping("/{orderId}/confirm")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> confirmPurchase(
            @PathVariable Long orderId,
            @LoginMember Long memberId) {
        orderService.purchaseConfirmation(memberId, orderId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "주문 상태 변경", description = "관리자 전용 배송 상태 전이(PAID -> PREPARING_FOR_SHIPMENT -> SHIPPING -> DELIVERED)")
    @PatchMapping("/{orderId}/state")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateOrderState(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStateUpdateRequestDto requestDto) {
        orderService.updateOrderStateByAdmin(orderId, requestDto.getOrderState());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "환불 요청 상태 변경", description = "관리자 전용 환불 요청 상태 전이(REQUESTED -> APPROVED|REJECTED)")
    @PatchMapping("/{orderId}/refund-request")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> processRefundRequest(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderRefundRequestProcessDto requestDto) {
        orderService.processRefundRequestByAdmin(orderId, requestDto.toDomainStatus(), requestDto.getProcessReason());
        return ResponseEntity.noContent().build();
    }

    //////////// @DELETE
}
