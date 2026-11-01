package kwh.cofshop.coupon.scheduler.outbox;

import kwh.cofshop.coupon.service.outbox.CouponIssueOutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponIssueOutboxSchedulerTest {

    @Mock
    private CouponIssueOutboxService outboxService;

    @InjectMocks
    private CouponIssueOutboxScheduler scheduler;

    @Test
    @DisplayName("발행 완료 행 정리: 보관 기간 기준으로 배치가 덜 찰 때까지 반복")
    void deleteSentEvents_repeatsUntilPartialBatch() {
        ReflectionTestUtils.setField(scheduler, "retentionDays", 7);
        when(outboxService.deleteSentEventsBefore(any(), anyInt())).thenReturn(1000, 1000, 3);

        scheduler.deleteSentEvents();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(outboxService, times(3)).deleteSentEventsBefore(cutoff.capture(), anyInt());
        assertThat(cutoff.getValue()).isCloseTo(LocalDateTime.now().minusDays(7), within(1, java.time.temporal.ChronoUnit.MINUTES));
    }
}
