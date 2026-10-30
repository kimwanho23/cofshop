package kwh.cofshop.coupon.redisstream.worker;

import kwh.cofshop.coupon.domain.CouponIssueState;
import kwh.cofshop.coupon.redisstream.service.LimitedCouponIssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingMessageCleanerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private LimitedCouponIssueService limitedCouponIssueService;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @InjectMocks
    private PendingMessageCleaner pendingMessageCleaner;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pendingMessageCleaner, "idleThresholdMs", 10_000L);
        ReflectionTestUtils.setField(pendingMessageCleaner, "maxRetryCount", 5);
    }

    @Test
    @DisplayName("pending 없음: 재처리 생략")
    void cleanPendingMessages_emptyPending() {
        mockOpsForStream();
        when(streamOperations.pending(anyString(), anyString(), any(), anyLong()))
                .thenReturn(new PendingMessages(CouponStreamConstants.COUPON_GROUP, List.of()));

        pendingMessageCleaner.cleanPendingMessages();

        verify(streamOperations, never()).claim(anyString(), anyString(), anyString(), any(Duration.class), any(RecordId.class));
        verifyNoInteractions(limitedCouponIssueService);
    }

    @Test
    @DisplayName("idle 시간이 임계값 미만이면 claim 하지 않음")
    void cleanPendingMessages_idleBelowThreshold_skip() {
        PendingMessage pending = pendingMessage("1-0", 5_000L, 1);
        mockOpsForStream();
        when(streamOperations.pending(anyString(), anyString(), any(), anyLong()))
                .thenReturn(new PendingMessages(CouponStreamConstants.COUPON_GROUP, List.of(pending)));

        pendingMessageCleaner.cleanPendingMessages();

        verify(streamOperations, never()).claim(anyString(), anyString(), anyString(), any(Duration.class), any(RecordId.class));
        verifyNoInteractions(limitedCouponIssueService);
    }

    @Test
    @DisplayName("SUCCESS 결과면 ack/delete")
    void cleanPendingMessages_success_ackAndDelete() {
        RecordId recordId = RecordId.of("2-0");
        PendingMessage pending = pendingMessage(recordId.getValue(), 12_000L, 1);
        MapRecord<String, String, String> claimedRecord = record(recordId, Map.of("memberId", "1", "couponId", "10"));

        mockOpsForStream();
        when(streamOperations.pending(anyString(), anyString(), any(), anyLong()))
                .thenReturn(new PendingMessages(CouponStreamConstants.COUPON_GROUP, List.of(pending)));
        when(streamOperations.claim(
                eq(CouponStreamConstants.STREAM_KEY),
                eq(CouponStreamConstants.COUPON_GROUP),
                eq("pending-cleaner"),
                any(Duration.class),
                eq(recordId)
        )).thenReturn((List) List.of(claimedRecord));
        when(limitedCouponIssueService.issueCoupon(10L, 1L)).thenReturn(CouponIssueState.SUCCESS);

        pendingMessageCleaner.cleanPendingMessages();

        verify(limitedCouponIssueService).issueCoupon(10L, 1L);
        verify(streamOperations).acknowledge(CouponStreamConstants.STREAM_KEY, CouponStreamConstants.COUPON_GROUP, recordId);
        verify(streamOperations).delete(CouponStreamConstants.STREAM_KEY, recordId);
        verify(streamOperations, never()).add(eq(CouponStreamConstants.DLQ_STREAM_KEY), any(Map.class));
    }

    @Test
    @DisplayName("STOCK_NOT_INITIALIZED 재시도 여유가 있으면 pending 유지")
    void cleanPendingMessages_stockNotInitialized_keepPending() {
        RecordId recordId = RecordId.of("3-0");
        PendingMessage pending = pendingMessage(recordId.getValue(), 12_000L, 4);
        MapRecord<String, String, String> claimedRecord = record(recordId, Map.of("memberId", "1", "couponId", "10"));

        mockOpsForStream();
        when(streamOperations.pending(anyString(), anyString(), any(), anyLong()))
                .thenReturn(new PendingMessages(CouponStreamConstants.COUPON_GROUP, List.of(pending)));
        when(streamOperations.claim(
                eq(CouponStreamConstants.STREAM_KEY),
                eq(CouponStreamConstants.COUPON_GROUP),
                eq("pending-cleaner"),
                any(Duration.class),
                eq(recordId)
        )).thenReturn((List) List.of(claimedRecord));
        when(limitedCouponIssueService.issueCoupon(10L, 1L)).thenReturn(CouponIssueState.STOCK_NOT_INITIALIZED);

        pendingMessageCleaner.cleanPendingMessages();

        verify(limitedCouponIssueService).issueCoupon(10L, 1L);
        verify(streamOperations, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
        verify(streamOperations, never()).delete(anyString(), any(RecordId.class));
        verify(streamOperations, never()).add(eq(CouponStreamConstants.DLQ_STREAM_KEY), any(Map.class));
    }

    @Test
    @DisplayName("STOCK_NOT_INITIALIZED 재시도 초과면 DLQ 이동 후 ack/delete")
    void cleanPendingMessages_stockNotInitialized_retryExceeded_moveDlq() {
        RecordId recordId = RecordId.of("4-0");
        PendingMessage pending = pendingMessage(recordId.getValue(), 12_000L, 5);
        MapRecord<String, String, String> claimedRecord = record(recordId, Map.of("memberId", "1", "couponId", "10"));

        mockOpsForStream();
        when(streamOperations.pending(anyString(), anyString(), any(), anyLong()))
                .thenReturn(new PendingMessages(CouponStreamConstants.COUPON_GROUP, List.of(pending)));
        when(streamOperations.claim(
                eq(CouponStreamConstants.STREAM_KEY),
                eq(CouponStreamConstants.COUPON_GROUP),
                eq("pending-cleaner"),
                any(Duration.class),
                eq(recordId)
        )).thenReturn((List) List.of(claimedRecord));
        when(limitedCouponIssueService.issueCoupon(10L, 1L)).thenReturn(CouponIssueState.STOCK_NOT_INITIALIZED);

        pendingMessageCleaner.cleanPendingMessages();

        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq(CouponStreamConstants.DLQ_STREAM_KEY), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).containsEntry("reason", "stock_not_initialized_retry_exceeded");
        assertThat(payloadCaptor.getValue()).containsEntry("originId", recordId.getValue());
        verify(streamOperations).acknowledge(CouponStreamConstants.STREAM_KEY, CouponStreamConstants.COUPON_GROUP, recordId);
        verify(streamOperations).delete(CouponStreamConstants.STREAM_KEY, recordId);
    }

    @Test
    @DisplayName("필드 값 파싱 실패면 DLQ 이동 후 ack/delete")
    void cleanPendingMessages_invalidField_moveDlq() {
        RecordId recordId = RecordId.of("5-0");
        PendingMessage pending = pendingMessage(recordId.getValue(), 12_000L, 1);
        MapRecord<String, String, String> claimedRecord = record(recordId, Map.of("memberId", "abc", "couponId", "10"));

        mockOpsForStream();
        when(streamOperations.pending(anyString(), anyString(), any(), anyLong()))
                .thenReturn(new PendingMessages(CouponStreamConstants.COUPON_GROUP, List.of(pending)));
        when(streamOperations.claim(
                eq(CouponStreamConstants.STREAM_KEY),
                eq(CouponStreamConstants.COUPON_GROUP),
                eq("pending-cleaner"),
                any(Duration.class),
                eq(recordId)
        )).thenReturn((List) List.of(claimedRecord));

        pendingMessageCleaner.cleanPendingMessages();

        verifyNoInteractions(limitedCouponIssueService);
        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq(CouponStreamConstants.DLQ_STREAM_KEY), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).containsEntry("reason", "invalid_fields");
        assertThat(payloadCaptor.getValue()).containsEntry("originId", recordId.getValue());
        verify(streamOperations).acknowledge(CouponStreamConstants.STREAM_KEY, CouponStreamConstants.COUPON_GROUP, recordId);
        verify(streamOperations).delete(CouponStreamConstants.STREAM_KEY, recordId);
    }

    @Test
    @DisplayName("재처리 예외 + 재시도 초과면 DLQ 이동 후 ack/delete")
    void cleanPendingMessages_exception_retryExceeded_moveDlq() {
        RecordId recordId = RecordId.of("6-0");
        PendingMessage pending = pendingMessage(recordId.getValue(), 12_000L, 5);
        MapRecord<String, String, String> claimedRecord = record(recordId, Map.of("memberId", "1", "couponId", "10"));

        mockOpsForStream();
        when(streamOperations.pending(anyString(), anyString(), any(), anyLong()))
                .thenReturn(new PendingMessages(CouponStreamConstants.COUPON_GROUP, List.of(pending)));
        when(streamOperations.claim(
                eq(CouponStreamConstants.STREAM_KEY),
                eq(CouponStreamConstants.COUPON_GROUP),
                eq("pending-cleaner"),
                any(Duration.class),
                eq(recordId)
        )).thenReturn((List) List.of(claimedRecord));
        when(limitedCouponIssueService.issueCoupon(10L, 1L)).thenThrow(new RuntimeException("boom"));

        pendingMessageCleaner.cleanPendingMessages();

        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq(CouponStreamConstants.DLQ_STREAM_KEY), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).containsEntry("reason", "retry_exceeded");
        verify(streamOperations).acknowledge(CouponStreamConstants.STREAM_KEY, CouponStreamConstants.COUPON_GROUP, recordId);
        verify(streamOperations).delete(CouponStreamConstants.STREAM_KEY, recordId);
    }

    private PendingMessage pendingMessage(String recordId, long idleMs, long deliveryCount) {
        return new PendingMessage(
                RecordId.of(recordId),
                Consumer.from(CouponStreamConstants.COUPON_GROUP, "consumer-1"),
                Duration.ofMillis(idleMs),
                deliveryCount
        );
    }

    @SuppressWarnings("unchecked")
    private MapRecord<String, String, String> record(RecordId id, Map<String, String> payload) {
        MapRecord<String, String, String> record = org.mockito.Mockito.mock(MapRecord.class);
        when(record.getId()).thenReturn(id);
        when(record.getValue()).thenReturn(payload);
        return record;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockOpsForStream() {
        when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOperations);
    }
}
