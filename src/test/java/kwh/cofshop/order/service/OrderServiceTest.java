package kwh.cofshop.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.LockModeType;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.dto.request.OrderCancelRequestDto;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.request.OrdererRequestDto;
import kwh.cofshop.order.dto.response.OrderCancelResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static kwh.cofshop.member.domain.QMember.member;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(10); // 10개의 스레드
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }


    @Test
    @DisplayName("주문 생성")
    @Transactional
    //@Commit
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

    }

    @DisplayName("멤버의 주문 목록")
    @Test
    @Transactional
    void getMemberOrders() throws JsonProcessingException {

        Member member = memberRepository.findByEmail("test2@gmail.com").orElseThrow();

        List<OrderResponseDto> orderResponseDtoList = orderService.memberOrders(member);

        log.info(objectMapper.writeValueAsString(orderResponseDtoList));
    }

    @DisplayName("하나의 주문 정보")
    @Test
    @Transactional
    void getItemInfo() throws JsonProcessingException {

        Member member = memberRepository.findByEmail("test2@gmail.com").orElseThrow();

        List<OrderResponseDto> orderResponseDtoList = orderService.memberOrders(member);
        log.info(objectMapper.writeValueAsString(orderResponseDtoList));

        for (OrderResponseDto orderResponseDto : orderResponseDtoList) {
            OrderResponseDto orderResponseDtoSet = orderService.orderSummary(orderResponseDto.getOrderId());
            log.info(objectMapper.writeValueAsString(orderResponseDtoSet));
            log.info("가르기");
        }
    }

    @DisplayName("주문 동시성 테스트")
    @Test
    @Transactional
    void testConcurrentOrderCreation() throws InterruptedException {

        Member member = memberRepository.findByEmail("test2@gmail.com").orElseThrow();
        Item item = itemRepository.findById(1L).orElseThrow();

        Address address = new Address("서울", "도시", "33145");
        OrdererRequestDto ordererRequestDto = new OrdererRequestDto();
        ordererRequestDto.setEmail(member.getEmail());
        ordererRequestDto.setAddress(address);

        OrderRequestDto orderRequestDto = getOrderRequestDto(item, ordererRequestDto);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            int count = i;
            executorService.execute(() -> {
                try {
                    orderService.createOrder(orderRequestDto, member);
                    log.info("{}번 실행", count);
                } catch (Exception e) {
                    System.err.println("Exception occurred: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // ✅ 모든 스레드 완료 대기
        latch.await();
        //2025 01 - 08
    //Exception occurred: could not execute statement [Deadlock found when trying to get lock; try restarting transaction] 트랜잭션 데드락 발생
        // ItemOption 조회 시 비관적 락 적용
    }

    private static OrderRequestDto getOrderRequestDto(Item item, OrdererRequestDto ordererRequestDto) {
        OrderItemRequestDto orderItem1 = new OrderItemRequestDto();
        orderItem1.setItem(item.getItemId());
        orderItem1.setQuantity(2);
        orderItem1.setOptionId(1L);

        OrderItemRequestDto orderItem2 = new OrderItemRequestDto();
        orderItem2.setItem(item.getItemId());
        orderItem2.setQuantity(2);
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

    @Test
    @DisplayName("주문 취소")
    @Transactional
    @Commit
    void OrderCancel() throws JsonProcessingException {
        OrderCancelRequestDto orderCancelRequestDto = new OrderCancelRequestDto();
        orderCancelRequestDto.setCancelReason("단순 변심");
        orderCancelRequestDto.setOrderId(34L);
        OrderCancelResponseDto orderCancelResponseDto = orderService.cancelOrder(orderCancelRequestDto);

        log.info(objectMapper.writeValueAsString(orderCancelResponseDto));

    }
}