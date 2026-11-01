package kwh.cofshop.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    String keyName() default "";

    long waitTime() default 10L;

    // -1이면 Redisson watchdog이 작업이 끝날 때까지 자동 연장. 프로세스가 죽으면 약 30초 뒤 풀림
    long leaseTime() default -1L;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    // 스케줄러용. 다른 서버가 실행 중이면 실패로 보지 않고 이번 실행을 건너뜀
    boolean skipIfLocked() default false;
}
