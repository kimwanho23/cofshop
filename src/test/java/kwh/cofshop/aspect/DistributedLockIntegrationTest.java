package kwh.cofshop.aspect;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.support.RedisIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 운영과 같이 스프링 자동 프록시로 애스펙트를 적용해 실제 Redis에서 검증
class DistributedLockIntegrationTest extends RedisIntegrationTestSupport {

    private static final int THREAD_COUNT = 8;
    private static final int OPERATIONS_PER_THREAD = 25;

    private AnnotationConfigApplicationContext context;
    private LockTarget proxy;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(AopConfig.class);
        context.registerBean(RedissonClient.class, RedisIntegrationTestSupport::redissonClient,
                definition -> definition.setDestroyMethodName(""));
        context.registerBean(DistributedLockAspect.class);
        context.registerBean(LockTarget.class);
        context.refresh();
        proxy = context.getBean(LockTarget.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("분산 락: 동시 호출을 직렬화해 값이 유실되지 않음")
    void concurrentCalls_areSerialized() throws Exception {
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int threadIndex = 0; threadIndex < THREAD_COUNT; threadIndex++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int operation = 0; operation < OPERATIONS_PER_THREAD; operation++) {
                        proxy.increment("shared");
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
        assertThat(proxy.getCounter()).isEqualTo(THREAD_COUNT * OPERATIONS_PER_THREAD);
    }

    @Test
    @DisplayName("분산 락: 다른 스레드가 잡고 있으면 대기 시간 후 실패하고, 풀리면 성공")
    void heldLock_failsThenSucceedsAfterRelease() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            RLock lock = redissonClient().getLock("LOCK:held:1");
            lock.lock();
            locked.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        holder.start();
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> proxy.tryOnce("1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.LOCK_ACQUISITION_FAILED);

        release.countDown();
        holder.join(10_000);

        assertThat(proxy.tryOnce("1")).isEqualTo("done");
    }

    @Test
    @DisplayName("분산 락: 같은 스레드의 중첩 호출은 재진입하고 끝나면 해제됨")
    void nestedCalls_reenterAndRelease() {
        assertThat(proxy.nested(proxy, "1")).isEqualTo("done");

        assertThat(redissonClient().getLock("LOCK:held:1").isLocked()).isFalse();
    }

    @Test
    @DisplayName("분산 락: 본문에서 예외가 나도 락이 해제됨")
    void exception_releasesLock() {
        assertThatThrownBy(() -> proxy.fail("1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("body failed");

        assertThat(redissonClient().getLock("LOCK:fail:1").isLocked()).isFalse();
    }

    @Test
    @DisplayName("분산 락: 트랜잭션을 감싸서 커밋이 끝난 뒤에 해제됨")
    void lock_isHeldUntilTransactionCommits() {
        proxy.transactional("1");

        RecordingTransactionManager transactionManager = context.getBean(RecordingTransactionManager.class);
        assertThat(transactionManager.lockHeldAtCommit).isTrue();
        assertThat(redissonClient().getLock("LOCK:tx:1").isLocked()).isFalse();
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class AopConfig {

        @Bean
        RecordingTransactionManager transactionManager() {
            return new RecordingTransactionManager();
        }
    }

    static class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        private volatile boolean lockHeldAtCommit;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            lockHeldAtCommit = redissonClient().getLock("LOCK:tx:1").isLocked();
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    public static class LockTarget {

        @DistributedLock(keyName = "'tx:' + #key")
        @Transactional
        public void transactional(String key) {
        }


        private int counter;

        public int getCounter() {
            return counter;
        }

        @DistributedLock(keyName = "'counter:' + #key")
        public void increment(String key) {
            int current = counter;
            Thread.yield();
            counter = current + 1;
        }

        @DistributedLock(keyName = "'held:' + #key", waitTime = 0)
        public String tryOnce(String key) {
            return "done";
        }

        @DistributedLock(keyName = "'held:' + #key")
        public String nested(LockTarget self, String key) {
            return self.tryOnce(key);
        }

        @DistributedLock(keyName = "'fail:' + #key")
        public void fail(String key) {
            throw new IllegalStateException("body failed");
        }
    }
}
