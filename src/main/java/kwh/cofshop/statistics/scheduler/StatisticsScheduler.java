package kwh.cofshop.statistics.scheduler;

import kwh.cofshop.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j

public class StatisticsScheduler {
    private final StatisticsService statisticsService;

    @Scheduled(cron = "0 0 0 * * *")
    public void saveLast7DaysTopItem() {
        statisticsService.refreshTopItemsLast7DaysCache();
        log.info("[Scheduler] : 7일간의 인기 상품 저장");
    }
}
