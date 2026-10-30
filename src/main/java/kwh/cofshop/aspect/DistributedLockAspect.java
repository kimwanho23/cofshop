package kwh.cofshop.aspect;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
// ExposeInvocationInterceptor(HIGHEST_PRECEDENCE + 1)보다 먼저 실행되면 어노테이션 인자 바인딩이 실패하고,
// 트랜잭션보다 늦게 실행되면 커밋 전에 락이 풀리므로 그 사이의 순서를 사용
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@RequiredArgsConstructor
public class DistributedLockAspect {

    private static final String LOCK_PREFIX = "LOCK:";

    private final RedissonClient redissonClient;

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        String lockKey = LOCK_PREFIX + CustomSpringELParser.getDynamicValue(
                signature.getParameterNames(),
                joinPoint.getArgs(),
                distributedLock.keyName()
        );

        RLock lock = redissonClient.getLock(lockKey); // 락 획득
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!isLocked) {
                log.warn("락 획득 실패 - {}", lockKey);
                throw new BusinessException(BusinessErrorCode.LOCK_ACQUISITION_FAILED);
            }
            log.info("락 획득 성공 - {}", lockKey);
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("락 대기 중 인터럽트 - {}", lockKey);
            throw new BusinessException(BusinessErrorCode.LOCK_ACQUISITION_FAILED);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("락 해제 - {}", lockKey);
            } else {
                log.debug("락 미보유 상태로 해제 생략 - {}", lockKey);
            }
        }
    }
}
