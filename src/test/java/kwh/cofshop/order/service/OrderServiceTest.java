package kwh.cofshop.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import kwh.cofshop.TestSettingUtils;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.dto.request.OrderCancelRequestDto;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.response.OrderCancelResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.mapper.OrderMapper;
import kwh.cofshop.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
class OrderServiceTest extends TestSettingUtils {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemOptionRepository itemOptionRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;


    private ExecutorService executorService;


    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(100); // 10개의 스레드
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }


    @Test
    @DisplayName("주문 생성")
    @Transactional
    void createOrder() throws Exception {
        Member member = createMember();
        Item item = createTestItem();

        OrderResponseDto order = orderService.createInstanceOrder(member.getId(),
                getOrderRequestDto(item));
        log.info(objectMapper.writeValueAsString(order));

    }

    @Test
    @DisplayName("주문 상태 변경")
    @Transactional
    void changeOrderState() throws Exception {
        Order order = orderRepository.findById(8188L).orElseThrow();
        order.changeOrderState(OrderState.SHIPPED);
        OrderResponseDto responseDto = orderMapper.toResponseDto(order);
        log.info(objectMapper.writeValueAsString(responseDto));
    }

    @Test
    @DisplayName("주문 취소")
    @Transactional
    void OrderCancel() throws JsonProcessingException {
        OrderCancelRequestDto orderCancelRequestDto = new OrderCancelRequestDto();
        orderCancelRequestDto.setCancelReason("단순 변심");
        orderCancelRequestDto.setOrderId(8188L);
        OrderCancelResponseDto orderCancelResponseDto = orderService.cancelOrder(orderCancelRequestDto);
        log.info(objectMapper.writeValueAsString(orderCancelResponseDto));

    }

    @DisplayName("멤버의 주문 목록")
    @Test
    @Transactional
    void getMemberOrders() throws JsonProcessingException {

        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponseDto> orderResponseDtoList = orderService.memberOrders(member.getId(), pageable);
        log.info(objectMapper.writeValueAsString(orderResponseDtoList));
    }

    @Test
    @DisplayName("모든 주문 목록")
    @Transactional
    void allOrders() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponseDto> orderResponseDto = orderService.allOrderList(pageable);
        objectMapper.writeValueAsString(orderResponseDto);

    }

    @Test
    @DisplayName("하나의 주문 정보")
    @Transactional
    void OrderSummary() throws JsonProcessingException {
        OrderResponseDto orderResponseDto = orderService.orderSummary(61L);
        log.info(objectMapper.writeValueAsString(orderResponseDto));
    }

    @DisplayName("멤버의 주문 정보")
    @Test
    @Transactional
    void getItemInfo() throws JsonProcessingException {

        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponseDto> orderResponseDtoList = orderService.memberOrders(member.getId(), pageable);

        log.info(objectMapper.writeValueAsString(orderResponseDtoList));

    }

    @DisplayName("주문 동시성 테스트")
    @Test
    void testConcurrentOrderCreation() throws InterruptedException {

        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        Long random = ThreadLocalRandom.current().nextLong(40, 330); // 1 이상, 4 미만
        Item item = itemRepository.findById(random).orElseThrow();

        int taskCount = 1000;
        CountDownLatch latch = new CountDownLatch(taskCount); // 1000개 완료 대기

        for (int i = 0; i < taskCount; i++) {
            int count = i;
            executorService.execute(() -> {
                try {
                    orderService.createInstanceOrder(member.getId(), getOrderRequestDto(item));
                    log.info("{}번 실행", count);
                } catch (Exception e) {
                    System.err.println("에러 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        //2025 01-08
        // Exception occurred: could not execute statement [Deadlock found when trying to get lock; try restarting transaction] 트랜잭션 데드락 발생
        // ItemOption 조회 시 비관적 락을 적용하여 해결
    }


    private OrderRequestDto getOrderRequestDto(Item item){
        OrderRequestDto orderRequestDto = new OrderRequestDto();
        orderRequestDto.setAddress(new Address("도시", "주소","우편번호"));
        orderRequestDto.setDeliveryRequest("배송 요청사항");
        orderRequestDto.setOrderItemRequestDtoList(getOrderItemRequestDto(item));
        orderRequestDto.setUsePoint(0);
        orderRequestDto.setDeliveryRequest("문 앞에 놓아주세요");
        return orderRequestDto;
    }

    // 주문 상품 정보
    private List<OrderItemRequestDto> getOrderItemRequestDto(Item item) {
        List<OrderItemRequestDto> dtoList = new ArrayList<>();

        int random = ThreadLocalRandom.current().nextInt(1, 5);

        List<ItemOption> itemOptions = item.getItemOptions();
        int size = itemOptions.size();

        int random2 = ThreadLocalRandom.current().nextInt(0, size);

        OrderItemRequestDto orderItem1 = new OrderItemRequestDto();
        orderItem1.setItem(item.getId());
        orderItem1.setQuantity(random);
        orderItem1.setOptionId(item.getItemOptions().get(random2).getId());
        dtoList.add(orderItem1);

        return dtoList;
    }

}