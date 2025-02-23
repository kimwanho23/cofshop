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
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @LoginMember CustomUserDetails customUserDetails,
            @Valid @RequestBody OrderRequestDto orderRequestDto){
        OrderResponseDto orderCreateResponseDto = orderService.createOrder(orderRequestDto, customUserDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Created(orderCreateResponseDto));
    }


    // 주문 취소
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderCancelResponseDto>> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderCancelRequestDto orderCancelRequestDto){
        OrderCancelResponseDto orderCancelResponseDto = orderService.cancelOrder(orderCancelRequestDto);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.OK(orderCancelResponseDto));
    }

    // 하나의 상품 주문 정보 조회
    @GetMapping("/{orderId}/info")
    public ResponseEntity<ApiResponse<OrderResponseDto>> orderInfo(
            @PathVariable Long orderId){
        OrderResponseDto orderResponseDto = orderService.orderSummary(orderId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.OK(orderResponseDto));
    }

    // 한 사람의 전체 주문 조회
    @GetMapping("/memberOrderList")
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> memberOrderList(
            @Valid @LoginMember CustomUserDetails customUserDetails,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Page<OrderResponseDto> orderResponseDto = orderService.memberOrders(customUserDetails.getId(), pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.OK(orderResponseDto));
    }

    @GetMapping("/allOrderList")
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> allOrderList(
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable){
        Page<OrderResponseDto> orderResponseDto = orderService.allOrderList(pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.OK(orderResponseDto));
    }
}
