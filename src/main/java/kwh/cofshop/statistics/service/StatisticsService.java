package kwh.cofshop.statistics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.statistics.dto.DailySalesDto;
import kwh.cofshop.statistics.dto.TopItemDto;
import kwh.cofshop.statistics.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final StatisticsRepository statisticsRepository;

    // 일별 판매량
    public List<DailySalesDto> getDailySales(LocalDate localDate) {
        return statisticsRepository.getDailySales(localDate);
    }

    // 최근 7일간 인기 상품
    public List<TopItemDto> getTopItemsLast7Days() {
        LocalDateTime current7days = LocalDateTime.now().minusDays(7);
        return statisticsRepository.getTopItemsLast7Days(current7days);
    }

    public List<TopItemDto> getTopItemsLast7DaysFromRedis() {
        String json = redisTemplate.opsForValue().get("ranking:top_items:last7days");
        return null; // 캐시 없음
    }

    // 기간 별 판매량
    public List<DailySalesDto> getDailySalesBetween(LocalDate from, LocalDate to) {
        return statisticsRepository.getDailySalesBetween(from, to);
    }
}
