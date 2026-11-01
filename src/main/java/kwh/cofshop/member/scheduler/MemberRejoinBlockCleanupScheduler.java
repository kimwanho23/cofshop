package kwh.cofshop.member.scheduler;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.member.service.MemberRejoinBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberRejoinBlockCleanupScheduler {

    private final MemberRejoinBlockService memberRejoinBlockService;

    @DistributedLock(keyName = "'member:rejoin-block:cleanup'", waitTime = 0, skipIfLocked = true)
    @Scheduled(cron = "${member.rejoin-block.cleanup-cron:0 20 4 * * *}")
    public void deleteExpiredBlocks() {
        int deleted = memberRejoinBlockService.deleteExpired();
        if (deleted > 0) {
            log.info("[RejoinBlock] deleted expired blocks count={}", deleted);
        }
    }
}
