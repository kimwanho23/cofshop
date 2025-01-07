package kwh.cofshop.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.request.OrdererRequestDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static kwh.cofshop.member.domain.QMember.member;

@SpringBootTest
@Slf4j
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemOptionRepository itemOptionRepository;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("주문 생성")
    @Transactional
   // @Commit
    void createOrder() throws Exception {

        Member member = memberRepository.findByEmail("test2@gmail.com").orElseThrow();
        Item item = itemRepository.findById(1L).orElseThrow();

        Address address = new Address("서울", "도시", "33145");
        OrdererRequestDto ordererRequestDto = new OrdererRequestDto();
        ordererRequestDto.setEmail(member.getEmail());
        ordererRequestDto.setAddress(address);

        OrderRequestDto orderRequestDto = getOrderRequestDto(item, ordererRequestDto);

        OrderResponseDto order = orderService.createOrder(orderRequestDto, member);
        log.info(objectMapper.writeValueAsString(order));
        log.info("Order ID: {}", order.getOrderId());

    }

    private static OrderRequestDto getOrderRequestDto(Item item, OrdererRequestDto ordererRequestDto) {
        OrderItemRequestDto orderItem1 = new OrderItemRequestDto();
        orderItem1.setOrderPrice(15000);
        orderItem1.setItem(item.getItemId());
        orderItem1.setQuantity(5);
        orderItem1.setOptionId(1L);

        OrderItemRequestDto orderItem2 = new OrderItemRequestDto();
        orderItem2.setOrderPrice(3000);
        orderItem2.setItem(item.getItemId());
        orderItem2.setQuantity(3);
        orderItem2.setOptionId(2L);

        OrderRequestDto orderRequestDto = new OrderRequestDto();

        orderRequestDto.setOrdererRequestDto(ordererRequestDto);

        List<OrderItemRequestDto> orderItemRequestDto = new ArrayList<>();
        orderItemRequestDto.add(orderItem1);
        orderItemRequestDto.add(orderItem2);
        orderRequestDto.setOrderItemRequestDtoList(orderItemRequestDto);

        return orderRequestDto;
    }

    @Test
    @DisplayName("주문 조회")
    @Transactional
    void OrderSummary() {
        orderService.orderSummary(2L);
    }
}