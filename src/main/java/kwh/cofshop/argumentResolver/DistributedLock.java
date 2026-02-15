package kwh.cofshop.argumentResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    // 키의 이름
    String keyName() default "";

    // 락 획득을 위한 대기 시간
    long waitTime() default 10L;

    // 락을 보유하는 시간
    long leaseTime() default 30L;

    // 시간 단위
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
