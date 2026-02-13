package kwh.cofshop.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import kwh.cofshop.argumentResolver.LoginMember;
import kwh.cofshop.order.dto.request.OrderCancelRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
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

    //////////// @DELETE
}
