package kwh.cofshop.cart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class CartItemServiceTest {

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
    @Commit
    void createCartItem() throws Exception {
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        Item item = itemRepository.findById(1L).orElseThrow();

        List<CartItemRequestDto> cartItemRequestDtoList = new ArrayList<>();

        CartItemRequestDto cartItemRequestDto1 = new CartItemRequestDto();
        cartItemRequestDto1.setItemId(item.getItemId());
        cartItemRequestDto1.setOptionId(1L);
        cartItemRequestDto1.setQuantity(1);
        cartItemRequestDtoList.add(cartItemRequestDto1);

        CartItemRequestDto cartItemRequestDto2 = new CartItemRequestDto();
        cartItemRequestDto2.setItemId(item.getItemId());
        cartItemRequestDto2.setOptionId(1L);
        cartItemRequestDto2.setQuantity(2);
        cartItemRequestDtoList.add(cartItemRequestDto2);

        CartItemRequestDto cartItemRequestDto3 = new CartItemRequestDto();
        cartItemRequestDto3.setItemId(item.getItemId());
        cartItemRequestDto3.setOptionId(2L);
        cartItemRequestDto3.setQuantity(2);
        cartItemRequestDtoList.add(cartItemRequestDto3);


        List<CartItemResponseDto> cartItemResponseDto = cartItemService.addCartItem(cartItemRequestDtoList, member);
        log.info(objectMapper.writeValueAsString(cartItemResponseDto));
    }


}