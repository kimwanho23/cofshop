package kwh.cofshop.statistics.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.coupon.service.CouponService;
import kwh.cofshop.coupon.service.MemberCouponService;
import kwh.cofshop.statistics.dto.TopItemDto;
import kwh.cofshop.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j

public class StatisticsScheduler {
    private final StatisticsService statisticsService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

        @Scheduled(cron = "0 0 0 * * *")
        public void saveLast7DaysTopItem() throws JsonProcessingException {
            List<TopItemDto> last7DaysTopItem = statisticsService.getTopItemsLast7Days();
            String data = objectMapper.writeValueAsString(last7DaysTopItem);
            redisTemplate.opsForValue().set("ranking:top_items:last7days", data, Duration.ofDays(1));

            log.info("[Scheduler] : 7일간의 인기 상품 조회");
        }
}
