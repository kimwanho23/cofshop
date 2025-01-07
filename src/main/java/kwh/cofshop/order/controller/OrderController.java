package kwh.cofshop.order.controller;

import jakarta.validation.Valid;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.dto.request.OrderCancelRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.response.OrderCancelResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @LoginMember Member member,
            @Valid @RequestBody OrderRequestDto orderRequestDto){
        OrderResponseDto orderCreateResponseDto = orderService.createOrder(orderRequestDto, member);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Created(orderCreateResponseDto));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderCancelResponseDto>> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderCancelRequestDto orderCancelRequestDto){
        OrderCancelResponseDto orderCancelResponseDto = orderService.cancelOrder(orderId, orderCancelRequestDto);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.OK(orderCancelResponseDto));
    }
}
