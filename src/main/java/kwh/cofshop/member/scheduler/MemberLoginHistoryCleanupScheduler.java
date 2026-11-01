package kwh.cofshop.member.scheduler;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.member.service.MemberLoginHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberLoginHistoryCleanupScheduler {

    private static final int BATCH_SIZE = 1000;

    private final MemberLoginHistoryService memberLoginHistoryService;

    @Value("${member.login-history.retention-months:3}")
    private int retentionMonths;

    @DistributedLock(keyName = "'member:login-history:cleanup'", waitTime = 0, skipIfLocked = true)
    @Scheduled(cron = "${member.login-history.cleanup-cron:0 0 4 * * *}")
    public void deleteExpiredHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(retentionMonths);
        int totalDeleted = 0;
        int deleted;
        do {
            deleted = memberLoginHistoryService.deleteLoginHistoryBefore(cutoff, BATCH_SIZE);
            totalDeleted += deleted;
        } while (deleted == BATCH_SIZE);
        if (totalDeleted > 0) {
            log.info("[LoginHistory] deleted expired history count={}", totalDeleted);
        }
    }
}
