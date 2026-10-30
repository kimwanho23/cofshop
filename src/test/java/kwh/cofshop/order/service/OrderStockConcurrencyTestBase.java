package kwh.cofshop.order.service;

import kwh.cofshop.config.QuerydslConfig;
import kwh.cofshop.config.auditing.JpaAuditingConfiguration;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.item.service.ItemReadPortAdapter;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.dto.request.OrderItemRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 스레드마다 별도 트랜잭션이 커밋되어야 하므로 테스트 트랜잭션을 사용하지 않음
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, JpaAuditingConfiguration.class, ItemReadPortAdapter.class, OrderItemService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class OrderStockConcurrencyTestBase {

    private static final int INITIAL_STOCK = 1_000;
    private static final int THREAD_COUNT = 8;
    private static final int OPERATIONS_PER_THREAD = 25;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ItemOptionRepository itemOptionRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("재고: 주문 차감과 취소 복구가 동시에 실행돼도 수량이 유실되지 않음")
    void orderAndRestoreConcurrently_keepsStock() throws Exception {
        ItemOption option = transactionTemplate.execute(status -> saveOption());
        Long itemId = option.getItem().getId();
        Long optionId = option.getId();

        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int threadIndex = 0; threadIndex < THREAD_COUNT; threadIndex++) {
            boolean ordering = threadIndex % 2 == 0;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int operation = 0; operation < OPERATIONS_PER_THREAD; operation++) {
                        transactionTemplate.executeWithoutResult(status -> {
                            if (ordering) {
                                order(itemId, optionId);
                            } else {
                                restore(optionId);
                            }
                        });
                    }
                } catch (Throwable e) {
                    failures.add(e);
                }
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(2, TimeUnit.MINUTES)).isTrue();

        assertThat(failures).isEmpty();
        assertThat(itemOptionRepository.findById(optionId).orElseThrow().getStock()).isEqualTo(INITIAL_STOCK);
    }

    private void order(Long itemId, Long optionId) {
        OrderItemRequestDto dto = new OrderItemRequestDto();
        dto.setItemId(itemId);
        dto.setOptionId(optionId);
        dto.setQuantity(1);
        List<OrderItemRequestDto> dtoList = new ArrayList<>(List.of(dto));

        orderItemService.createOrderItems(dtoList, orderItemService.getItemOptionsWithLock(dtoList));
    }

    private void restore(Long optionId) {
        ItemOption option = ItemOption.builder().id(optionId).build();
        OrderItem orderItem = OrderItem.builder().itemOption(option).quantity(1).build();

        orderItemService.restoreStock(List.of(orderItem));
    }

    private ItemOption saveOption() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Member seller = memberRepository.save(Member.builder()
                .email(unique + "@example.com")
                .memberName("판매자")
                .memberPwd("pw")
                .tel("010" + unique)
                .build());
        Item item = itemRepository.save(Item.builder()
                .itemName("커피")
                .price(1000)
                .deliveryFee(0)
                .origin("브라질")
                .seller(seller)
                .build());
        return itemOptionRepository.save(ItemOption.createOption("기본", 0, INITIAL_STOCK, item));
    }
}
