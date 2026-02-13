package kwh.cofshop.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.dto.request.OrderCancelRequestDto;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.response.OrderCancelResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.service.OrderService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import kwh.cofshop.support.TestLoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        requestDto.setAddress(Address.builder()
                .city("서울")
                .street("강남대로")
                .zipCode("12345")
                .build());
        requestDto.setOrderItemRequestDtoList(List.of(orderItemRequestDto));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("주문 취소")
    void cancelOrder() throws Exception {
        OrderCancelResponseDto responseDto = new OrderCancelResponseDto();
        responseDto.setOrderId(1L);

        when(orderService.cancelOrder(anyLong(), anyLong(), any())).thenReturn(responseDto);

        OrderCancelRequestDto requestDto = new OrderCancelRequestDto();
        requestDto.setCancelReason("변경");

        mockMvc.perform(patch("/api/orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("구매 확정")
    void confirmPurchase() throws Exception {
        mockMvc.perform(patch("/api/orders/1/confirm"))
                .andExpect(status().isNoContent());
    }
}
