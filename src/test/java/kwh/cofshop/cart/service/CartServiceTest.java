package kwh.cofshop.cart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @DisplayName("장바구니 조회")
    @Transactional
    void getCart() throws JsonProcessingException {
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();

        CartResponseDto memberCartItems = cartService.getMemberCartItems(member);
        log.info(objectMapper.writeValueAsString(memberCartItems));
    }


}