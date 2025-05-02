package kwh.cofshop.cart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.TestSettingUtils;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.item.domain.Item;

import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class CartItemServiceTest extends TestSettingUtils {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemService cartItemService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("장바구니 생성")
    @Transactional
    void createCartItem() throws Exception {
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        Item item = itemRepository.findById(2L).orElseThrow();

        List<CartItemRequestDto> cartItemRequestDtoList = getCartItemRequestDto(item);


        List<CartItemResponseDto> cartItemResponseDto = cartItemService.addCartItemList(cartItemRequestDtoList, member.getId());
        log.info(objectMapper.writeValueAsString(cartItemResponseDto));
    }
    @Test
    @DisplayName("장바구니 조회")
    @Transactional
    void getCart() throws JsonProcessingException {
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        List<CartItemResponseDto> cartItemResponseDtoList = cartItemService.getCartItemsByMemberId(member.getId());
        log.info(objectMapper.writeValueAsString(cartItemResponseDtoList));
    }

}