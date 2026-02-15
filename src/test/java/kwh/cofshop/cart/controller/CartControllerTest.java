package kwh.cofshop.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.cart.service.CartItemService;
import kwh.cofshop.cart.service.CartService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import kwh.cofshop.support.TestLoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private CartService cartService;

    @Mock
    private CartItemService cartItemService;

    @InjectMocks
    private CartController cartController;

    @InjectMocks
    private CartItemController cartItemController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = StandaloneMockMvcFactory.build(
                new Object[]{cartController, cartItemController},
                new TestLoginMemberArgumentResolver()
        );
    }

    @Test
    @DisplayName("장바구니 존재 여부 확인")
    void checkCartExists() throws Exception {
        when(cartService.checkCartExistByMemberId(anyLong())).thenReturn(true);

        mockMvc.perform(get("/api/carts/me/exists"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("장바구니 상품 목록 추가")
    void addCartItems() throws Exception {
        CartItemResponseDto responseDto = new CartItemResponseDto();
        responseDto.setOptionId(10L);

        when(cartItemService.addCartItemList(anyList(), anyLong()))
                .thenReturn(List.of(responseDto));

        CartItemRequestDto requestDto = new CartItemRequestDto();
        requestDto.setItemId(1L);
        requestDto.setOptionId(10L);
        requestDto.setQuantity(2);

        mockMvc.perform(post("/api/cart-items/me/items/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(requestDto))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("장바구니 목록 조회")
    void getCartItems() throws Exception {
        when(cartItemService.getCartItemsByMemberId(anyLong()))
                .thenReturn(List.of(new CartItemResponseDto()));

        mockMvc.perform(get("/api/cart-items/me"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("장바구니 총 금액 조회")
    void getTotalPrice() throws Exception {
        when(cartItemService.calculateTotalPrice(anyLong())).thenReturn(1000);

        mockMvc.perform(get("/api/cart-items/me/total-price"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("장바구니 상품 추가")
    void addCartItem() throws Exception {
        CartItemResponseDto responseDto = new CartItemResponseDto();
        responseDto.setOptionId(10L);

        when(cartItemService.addCartItem(any(), anyLong())).thenReturn(responseDto);

        CartItemRequestDto requestDto = new CartItemRequestDto();
        requestDto.setItemId(1L);
        requestDto.setOptionId(10L);
        requestDto.setQuantity(1);

        mockMvc.perform(post("/api/cart-items/me/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("장바구니 수량 변경")
    void updateQuantity() throws Exception {
        CartItemRequestDto requestDto = new CartItemRequestDto();
        requestDto.setItemId(1L);
        requestDto.setOptionId(10L);
        requestDto.setQuantity(3);

        mockMvc.perform(patch("/api/cart-items/me/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("장바구니 단일 상품 삭제")
    void deleteCartItem() throws Exception {
        mockMvc.perform(delete("/api/cart-items/me/items/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("장바구니 전체 상품 삭제")
    void deleteAllCartItems() throws Exception {
        mockMvc.perform(delete("/api/cart-items/me/items"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("장바구니 생성")
    void createCart() throws Exception {
        CartResponseDto responseDto = new CartResponseDto();
        responseDto.setId(1L);

        when(cartService.createCart(anyLong())).thenReturn(responseDto);

        mockMvc.perform(post("/api/carts/me"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("장바구니 삭제")
    void deleteCart() throws Exception {
        mockMvc.perform(delete("/api/carts/me"))
                .andExpect(status().isNoContent());
    }
}
