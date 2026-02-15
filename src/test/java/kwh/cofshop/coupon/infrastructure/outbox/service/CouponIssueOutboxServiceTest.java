package kwh.cofshop.coupon.infrastructure.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.coupon.application.port.out.CouponIssueEventPublisher;
import kwh.cofshop.coupon.application.port.out.message.CouponIssueEventMessage;
import kwh.cofshop.coupon.infrastructure.outbox.persistence.entity.CouponIssueOutboxEvent;
import kwh.cofshop.coupon.infrastructure.outbox.persistence.entity.CouponIssueOutboxStatus;
import kwh.cofshop.coupon.infrastructure.outbox.persistence.repository.CouponIssueOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponIssueOutboxServiceTest {

    @Mock
    private CouponIssueOutboxRepository outboxRepository;

    @Mock
    private CouponIssueEventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CouponIssueOutboxService outboxService;

    @Test
    @DisplayName("발급 완료 outbox 적재")
    void enqueueCouponIssued() {
        outboxService.enqueueCouponIssued(11L, 1L, 5L);

        ArgumentCaptor<CouponIssueOutboxEvent> captor = ArgumentCaptor.forClass(CouponIssueOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        CouponIssueOutboxEvent event = captor.getValue();
        assertThat(event.getStatus()).isEqualTo(CouponIssueOutboxStatus.PENDING);
        assertThat(event.getMemberCouponId()).isEqualTo(11L);
        assertThat(event.getMemberId()).isEqualTo(1L);
        assertThat(event.getCouponId()).isEqualTo(5L);
        assertThat(event.getPayload()).contains("\"memberCouponId\":11");
    }

    @Test
    @DisplayName("대기 outbox 발행 성공 시 SENT")
    void publishPendingEvents_success() {
        CouponIssueOutboxEvent pending = CouponIssueOutboxEvent.pending(11L, 1L, 5L,
                "{\"memberCouponId\":11,\"memberId\":1,\"couponId\":5}");
        ReflectionTestUtils.setField(pending, "id", 99L);

        when(outboxRepository.findByStatusOrderByIdAsc(any(CouponIssueOutboxStatus.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pending)));

        int result = outboxService.publishPendingEvents(10, 5);

        assertThat(result).isEqualTo(1);
        assertThat(pending.getStatus()).isEqualTo(CouponIssueOutboxStatus.SENT);
        verify(eventPublisher).publish(new CouponIssueEventMessage(99L, 11L, 1L, 5L));
    }

    @Test
    @DisplayName("대기 outbox 발행 실패 시 retry 증가 후 한도 초과면 FAILED")
    void publishPendingEvents_failed() {
        CouponIssueOutboxEvent pending = CouponIssueOutboxEvent.pending(11L, 1L, 5L,
                "{\"memberCouponId\":11,\"memberId\":1,\"couponId\":5}");
        ReflectionTestUtils.setField(pending, "id", 100L);

        when(outboxRepository.findByStatusOrderByIdAsc(any(CouponIssueOutboxStatus.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pending)));
        doThrow(new RuntimeException("publish fail"))
                .when(eventPublisher)
                .publish(any(CouponIssueEventMessage.class));

        int result = outboxService.publishPendingEvents(10, 1);

        assertThat(result).isEqualTo(0);
        assertThat(pending.getRetryCount()).isEqualTo(1);
        assertThat(pending.getStatus()).isEqualTo(CouponIssueOutboxStatus.FAILED);
    }
}
