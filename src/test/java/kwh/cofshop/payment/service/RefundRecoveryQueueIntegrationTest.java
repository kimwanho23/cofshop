package kwh.cofshop.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService.ClaimedRecoveryTask;
import kwh.cofshop.support.RedisIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RefundRecoveryQueueIntegrationTest extends RedisIntegrationTestSupport {

    private RefundCompensationRecoveryQueueService queueService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        queueService = new RefundCompensationRecoveryQueueService(redisTemplate(), objectMapper);
    }

    @Test
    @DisplayName("복구 큐: 꺼낸 작업을 ack하면 다시 나오지 않음")
    void dequeueAndAcknowledge() {
        queueService.enqueue(10L, 1L, 100L, "REASON");

        List<ClaimedRecoveryTask> claimed = queueService.dequeueBatch(10);

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).task().paymentId()).isEqualTo(10L);
        assertThat(claimed.get(0).task().orderId()).isEqualTo(100L);

        queueService.acknowledge(claimed.get(0));

        assertThat(queueService.dequeueBatch(10)).isEmpty();
    }

    @Test
    @DisplayName("복구 큐: ack 전에 중단된 작업은 다음 조회에서 복원")
    void unacknowledged_isRestored() {
        queueService.enqueue(10L, 1L, 100L, "REASON");
        queueService.dequeueBatch(10);

        List<ClaimedRecoveryTask> restored = queueService.dequeueBatch(10);

        assertThat(restored).hasSize(1);
        assertThat(restored.get(0).task().paymentId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("복구 큐: 재적재하면 재시도 횟수가 늘고 작업은 한 건만 남음")
    void requeue_incrementsRetry() {
        queueService.enqueue(10L, 1L, 100L, "REASON");

        queueService.requeue(queueService.dequeueBatch(10).get(0), "NEXT");

        List<ClaimedRecoveryTask> claimed = queueService.dequeueBatch(10);
        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).task().retryCount()).isEqualTo(1);
        assertThat(claimed.get(0).task().reasonCode()).isEqualTo("NEXT");
    }

    @Test
    @DisplayName("복구 큐: DLQ로 옮긴 작업을 재처리하면 본 큐로 돌아옴")
    void deadLetter_replay() {
        queueService.enqueue(10L, 1L, 100L, "REASON");

        queueService.deadLetter(queueService.dequeueBatch(10).get(0), "FAILED");

        assertThat(queueService.deadLetterSize()).isEqualTo(1L);
        assertThat(queueService.dequeueBatch(10)).isEmpty();

        assertThat(queueService.replayDeadLetters(10)).isEqualTo(1);

        assertThat(queueService.deadLetterSize()).isZero();
        assertThat(queueService.dequeueBatch(10)).hasSize(1);
    }
}
