package kwh.cofshop.order.scheduler;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.order.service.OrderWithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPersonalArchiveScheduler {

    private final OrderWithdrawalService orderWithdrawalService;

    @DistributedLock(keyName = "'order:personal-archive:cleanup'", waitTime = 0, skipIfLocked = true)
    @Scheduled(cron = "${order.personal-archive.cleanup-cron:0 10 4 * * *}")
    public void deleteExpiredArchives() {
        int deleted = orderWithdrawalService.deleteExpiredArchives();
        if (deleted > 0) {
            log.info("[OrderPersonalArchive] deleted expired archives count={}", deleted);
        }
    }
}
