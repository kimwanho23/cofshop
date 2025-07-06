package kwh.cofshop.payment.service;

import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.dto.PaymentPrepareRequestDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class PaymentServiceIntegrationTest {

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Autowired
    private PaymentService paymentService;
    /**
     * 결제 분산 락 테스트
     *
     */

    @Test
    @DisplayName("분산 락 결제")
    void Distributed_Lock_payment_Test() throws Exception {
        // given
        Long orderId = 11L;

        // 멀티스레드 테스트
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    PaymentPrepareRequestDto dto = PaymentPrepareRequestDto.builder()
                            .pgProvider("kakaopay")
                            .payMethod("card")
                            .build();
                    paymentService.createPaymentRequest(orderId, dto);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 스레드 종료 대기
        verify(paymentEntityRepository, atMost(1)).save(any(PaymentEntity.class));
        log.info("성공 COUNT - {}", successCount.get());
        log.info("예외 COUNT - {}", errorCount.get());
        }


    @Test
    @DisplayName("분산 락 없는 결제")
    void payment_Test() throws Exception {
        // given
        Long orderId = 6L;

        // 멀티스레드 테스트
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    PaymentPrepareRequestDto dto = PaymentPrepareRequestDto.builder()
                            .pgProvider("kakaopay")
                            .payMethod("card")
                            .build();
                    paymentService.createPaymentRequestTest(orderId, dto);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 스레드 종료 대기
        verify(paymentEntityRepository, atMost(1)).save(any(PaymentEntity.class));
        log.info("성공 COUNT - {}", successCount.get());
        log.info("예외 COUNT - {}", errorCount.get());
    }


}
