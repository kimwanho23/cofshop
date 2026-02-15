package kwh.cofshop.coupon.infrastructure.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.coupon.application.port.out.CouponIssueEventPublisher;
import kwh.cofshop.coupon.application.port.out.message.CouponIssueEventMessage;
import kwh.cofshop.coupon.infrastructure.outbox.persistence.entity.CouponIssueOutboxEvent;
import kwh.cofshop.coupon.infrastructure.outbox.persistence.entity.CouponIssueOutboxStatus;
import kwh.cofshop.coupon.infrastructure.outbox.persistence.repository.CouponIssueOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponIssueOutboxService {

    private final CouponIssueOutboxRepository outboxRepository;
    private final CouponIssueEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueueCouponIssued(Long memberCouponId, Long memberId, Long couponId) {
        String payload = serialize(new CouponIssuePayload(memberCouponId, memberId, couponId));
        CouponIssueOutboxEvent event = CouponIssueOutboxEvent.pending(memberCouponId, memberId, couponId, payload);
        outboxRepository.save(event);
    }

    @Transactional
    public int publishPendingEvents(int batchSize, int maxRetryCount) {
        int safeBatchSize = Math.max(1, batchSize);
        int safeMaxRetryCount = Math.max(1, maxRetryCount);

        List<CouponIssueOutboxEvent> events = outboxRepository
                .findByStatusOrderByIdAsc(
                        CouponIssueOutboxStatus.PENDING,
                        PageRequest.of(0, safeBatchSize)
                )
                .getContent();

        if (events.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (CouponIssueOutboxEvent event : events) {
            try {
                CouponIssuePayload payload = deserialize(event.getPayload());
                eventPublisher.publish(new CouponIssueEventMessage(
                        event.getId(),
                        payload.memberCouponId(),
                        payload.memberId(),
                        payload.couponId()
                ));
                event.markSent();
                successCount++;
            } catch (Exception e) {
                event.markFailed(e.getMessage(), safeMaxRetryCount);
                log.warn("[CouponOutbox] publish failed eventId={}, retryCount={}, error={}",
                        event.getId(), event.getRetryCount(), e.getMessage());
            }
        }
        return successCount;
    }

    private String serialize(CouponIssuePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize coupon outbox payload", e);
        }
    }

    private CouponIssuePayload deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, CouponIssuePayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize coupon outbox payload", e);
        }
    }

    private record CouponIssuePayload(Long memberCouponId, Long memberId, Long couponId) {
    }
}
