package kwh.cofshop.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import kwh.cofshop.order.dto.request.AddressRequestDto;
import kwh.cofshop.order.dto.request.OrderCancelRequestDto;
import kwh.cofshop.order.dto.request.OrderRefundRequestProcessDto;
import kwh.cofshop.order.dto.request.OrderRefundRequestProcessStatus;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.request.OrderStateUpdateRequestDto;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.dto.response.OrderCancelResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.service.OrderService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import kwh.cofshop.support.TestLoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = StandaloneMockMvcFactory.build(
                orderController,
                new TestLoginMemberArgumentResolver()
        );
    }

    @Test
    @DisplayName("주문 상세 조회")
    void getOrderInfo() throws Exception {
        when(orderService.orderSummary(anyLong(), anyLong())).thenReturn(new OrderResponseDto());

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 주문 목록 조회")
    void getMyOrders() throws Exception {
        when(orderService.memberOrders(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(new OrderResponseDto()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/orders/me")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 주문 목록 조회")
    void getAllOrders() throws Exception {
        when(orderService.allOrderList(any()))
                .thenReturn(new PageImpl<>(List.of(new OrderResponseDto()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("주문 생성")
    void createOrder() throws Exception {
        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setOrderId(1L);

        when(orderService.createInstanceOrder(anyLong(), any())).thenReturn(responseDto);

        OrderItemRequestDto orderItemRequestDto = new OrderItemRequestDto();
        orderItemRequestDto.setItemId(1L);
        orderItemRequestDto.setOptionId(10L);
        orderItemRequestDto.setQuantity(2);

        OrderRequestDto requestDto = new OrderRequestDto();
        AddressRequestDto addressRequestDto = new AddressRequestDto();
        addressRequestDto.setCity("서울");
        addressRequestDto.setStreet("강남대로");
        addressRequestDto.setZipCode("12345");
        requestDto.setAddress(addressRequestDto);
        requestDto.setOrderItems(List.of(orderItemRequestDto));
        requestDto.setMemberCouponId(500L);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        ArgumentCaptor<OrderRequestDto> captor = ArgumentCaptor.forClass(OrderRequestDto.class);
        verify(orderService).createInstanceOrder(eq(1L), captor.capture());
        assertThat(captor.getValue().getMemberCouponId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("주문 생성: 유효하지 않은 memberCouponId면 409")
    void createOrder_invalidMemberCouponId() throws Exception {
        when(orderService.createInstanceOrder(anyLong(), any()))
                .thenThrow(new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE));

        OrderItemRequestDto orderItemRequestDto = new OrderItemRequestDto();
        orderItemRequestDto.setItemId(1L);
        orderItemRequestDto.setOptionId(10L);
        orderItemRequestDto.setQuantity(2);

        OrderRequestDto requestDto = new OrderRequestDto();
        AddressRequestDto addressRequestDto = new AddressRequestDto();
        addressRequestDto.setCity("서울");
        addressRequestDto.setStreet("강남대로");
        addressRequestDto.setZipCode("12345");
        requestDto.setAddress(addressRequestDto);
        requestDto.setOrderItems(List.of(orderItemRequestDto));
        requestDto.setMemberCouponId(10L);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("주문 취소")
    void cancelOrder() throws Exception {
        OrderCancelResponseDto responseDto = OrderCancelResponseDto.of(1L, "변경");

        when(orderService.cancelOrder(anyLong(), anyLong(), any())).thenReturn(responseDto);

        OrderCancelRequestDto requestDto = new OrderCancelRequestDto();
        requestDto.setCancelReason("변경");

        mockMvc.perform(patch("/api/orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("환불 요청")
    void requestRefundRequest() throws Exception {
        mockMvc.perform(post("/api/orders/1/refund-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refundRequestReason":"상품 상태 불량"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(orderService).requestRefundRequest(1L, 1L, "상품 상태 불량");
    }

    @Test
    @DisplayName("환불 요청: 사유 누락은 400")
    void requestRefundRequest_withoutReason() throws Exception {
        mockMvc.perform(post("/api/orders/1/refund-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refundRequestReason":""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구매 확정")
    void confirmPurchase() throws Exception {
        mockMvc.perform(patch("/api/orders/1/confirm"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 주문 상태 변경")
    void updateOrderState() throws Exception {
        OrderStateUpdateRequestDto requestDto = new OrderStateUpdateRequestDto();
        requestDto.setOrderState(OrderState.SHIPPING);

        mockMvc.perform(patch("/api/orders/1/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNoContent());

        verify(orderService).updateOrderStateByAdmin(1L, OrderState.SHIPPING);
    }

    @Test
    @DisplayName("관리자 주문 상태 변경: 환불 요청 진행 중이면 409 + 전용 에러코드")
    void updateOrderState_refundRequestInProgress_conflict() throws Exception {
        doThrow(new BusinessException(BusinessErrorCode.ORDER_REFUND_REQUEST_IN_PROGRESS))
                .when(orderService).updateOrderStateByAdmin(anyLong(), any());

        OrderStateUpdateRequestDto requestDto = new OrderStateUpdateRequestDto();
        requestDto.setOrderState(OrderState.PREPARING_FOR_SHIPMENT);

        mockMvc.perform(patch("/api/orders/1/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value(BusinessErrorCode.ORDER_REFUND_REQUEST_IN_PROGRESS.getCode()));
    }

    @Test
    @DisplayName("관리자 환불 요청 상태 변경")
    void processRefundRequest() throws Exception {
        OrderRefundRequestProcessDto requestDto = new OrderRefundRequestProcessDto();
        requestDto.setRefundRequestStatus(OrderRefundRequestProcessStatus.APPROVED);
        requestDto.setProcessReason("환불 승인");

        mockMvc.perform(patch("/api/orders/1/refund-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNoContent());

        verify(orderService).processRefundRequestByAdmin(1L, OrderRefundRequestStatus.APPROVED, "환불 승인");
    }

    @Test
    @DisplayName("관리자 환불 요청 상태 변경: REFUNDED 입력은 400")
    void processRefundRequest_refundedBadRequest() throws Exception {
        mockMvc.perform(patch("/api/orders/1/refund-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refundRequestStatus":"REFUNDED",
                                  "processReason":"환불 완료"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
