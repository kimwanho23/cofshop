package kwh.cofshop.order.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.order.dto.request.OrderCancelRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.response.OrderCancelResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.service.OrderService;
import kwh.cofshop.security.CustomUserDetails;
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
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderInfo(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.OK(orderService.orderSummary(orderId)));
    }

    // 현재 자신의 전체 주문 조회
    @GetMapping("/me")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getMyOrders(
            @LoginMember CustomUserDetails user,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<OrderResponseDto> responseDto = orderService.memberOrders(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.OK(responseDto));
    }


    // 모든 주문 조회
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getAllOrders(
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderResponseDto> responseDto = orderService.allOrderList(pageable);
        return ResponseEntity.ok(ApiResponse.OK(responseDto));
    }

    //////////// @POST
    // 주문 생성
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @LoginMember CustomUserDetails user,
            @Valid @RequestBody OrderRequestDto requestDto) {

        OrderResponseDto responseDto = orderService.createInstanceOrder(user.getId(), requestDto);

        return ResponseEntity.created(URI.create("/api/orders/" + responseDto.getOrderId()))
                .body(ApiResponse.Created(responseDto));
    }


    //////////// @PUT, PATCH
    // 주문 취소
    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ApiResponse<OrderCancelResponseDto>> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderCancelRequestDto dto) {

        OrderCancelResponseDto responseDto = orderService.cancelOrder(dto);
        return ResponseEntity.ok(ApiResponse.OK(responseDto));
    }

    // 구매 확정
    @PatchMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPurchase(@PathVariable Long orderId) {
        orderService.PurchaseConfirmation(orderId);
        return ResponseEntity.ok().build();
    }

    //////////// @DELETE
}
