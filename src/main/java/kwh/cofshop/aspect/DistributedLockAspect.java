package kwh.cofshop.aspect;

import kwh.cofshop.argumentResolver.DistributedLock;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private static final String LOCK_PREFIX = "LOCK:";

    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;

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
                throw new BadRequestException(BadRequestErrorCode.BAD_REQUEST);
            }
            log.info("락 획득 성공 - {}", lockKey);
            return aopForTransaction.proceed(joinPoint);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("락 대기 중 인터럽트 - {}", lockKey);
            throw new BadRequestException(BadRequestErrorCode.BAD_REQUEST);
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
