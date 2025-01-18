package kwh.cofshop.cart.controller;

import kwh.cofshop.ControllerTestSetting;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;  // POST 요청
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
class CartControllerTest extends ControllerTestSetting {


    @Autowired
    private ItemRepository itemRepository;


    @Test
    @DisplayName("장바구니에 항목 추가")
    @Transactional
    public void addCart() throws Exception {

        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        Item item = itemRepository.findById(1L).orElseThrow();
        List<CartItemRequestDto> requestDto = getCartItemRequestDto(item);

        MvcResult result = mockMvc.perform(post("/api/cart/addCart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .header("Authorization", "Bearer " + getToken()))
                .andExpect(status().isCreated())
                .andDo(print())
                .andReturn();
    }

    @Test
    @DisplayName("장바구니 가져오기")
    @Transactional
    public void getCart() throws Exception {
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        Authentication testAuthentication = createTestAuthentication(member);
        String accessToken = jwtTokenProvider.createAuthToken(testAuthentication).getAccessToken();

        MvcResult result = mockMvc.perform(get("/api/cart/getCart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
    }

    public Authentication createTestAuthentication(Member member) {
        Collection<? extends GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + member.getRole())
        );

        CustomUserDetails userDetails = new CustomUserDetails(
                member.getEmail(),
                "",
                authorities,
                member.getMemberState(),
                member.getLastPasswordChange()
        );

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }


    private static List<CartItemRequestDto> getCartItemRequestDto(Item item) {
        List<CartItemRequestDto> cartItemRequestDtoList = new ArrayList<>();

        CartItemRequestDto cartItemRequestDto1 = new CartItemRequestDto();
        cartItemRequestDto1.setItemId(item.getId());
        cartItemRequestDto1.setOptionId(1L);
        cartItemRequestDto1.setQuantity(1);
        cartItemRequestDtoList.add(cartItemRequestDto1);

        CartItemRequestDto cartItemRequestDto2 = new CartItemRequestDto();
        cartItemRequestDto2.setItemId(item.getId());
        cartItemRequestDto2.setOptionId(1L);
        cartItemRequestDto2.setQuantity(2);
        cartItemRequestDtoList.add(cartItemRequestDto2);

        CartItemRequestDto cartItemRequestDto3 = new CartItemRequestDto();
        cartItemRequestDto3.setItemId(item.getId());
        cartItemRequestDto3.setOptionId(2L);
        cartItemRequestDto3.setQuantity(2);
        cartItemRequestDtoList.add(cartItemRequestDto3);
        return cartItemRequestDtoList;
    }
}