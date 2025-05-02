package kwh.cofshop.cart.controller;

import kwh.cofshop.TestSettingUtils;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.repository.ItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;  // POST 요청
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@Slf4j
class CartControllerTest extends TestSettingUtils {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @DisplayName("장바구니에 항목 추가")
    @Transactional
    public void addCart() throws Exception {
        Item item = createTestItem();
        List<CartItemRequestDto> requestDto = getCartItemRequestDto(item);

        MvcResult result = mockMvc.perform(post("/api/cart-items/me/items/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .header("Authorization", "Bearer " + getToken()))
                .andExpect(status().isCreated())
                .andDo(print())
                .andReturn();
    }

    @Test
    @DisplayName("장바구니 아이템 가져오기")
    @Transactional
    public void getCart() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cart-items/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + getToken()))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
    }



}