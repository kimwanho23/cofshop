package kwh.cofshop.statistics.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.statistics.dto.DailySalesResponseDto;
import kwh.cofshop.statistics.dto.TopItemResponseDto;
import kwh.cofshop.statistics.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private static final String TOP_ITEMS_LAST_7_DAYS_KEY = "ranking:top_items:last7days";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final StatisticsRepository statisticsRepository;

    // 일별 판매량
    public List<DailySalesResponseDto> getDailySales(LocalDate localDate) {
        return statisticsRepository.getDailySales(localDate);
    }

    // 최근 7일간 인기 상품
    public List<TopItemResponseDto> getTopItemsLast7Days() {
        List<TopItemResponseDto> cachedTopItems = getTopItemsLast7DaysFromRedis();
        if (!cachedTopItems.isEmpty()) {
            return cachedTopItems;
        }

        return refreshTopItemsLast7DaysCache();
    }

    public List<TopItemResponseDto> refreshTopItemsLast7DaysCache() {
        LocalDateTime current7days = LocalDateTime.now().minusDays(7);
        List<TopItemResponseDto> topItems = statisticsRepository.getTopItemsLast7Days(current7days);
        cacheTopItemsLast7Days(topItems);
        return topItems;
    }

    public List<TopItemResponseDto> getTopItemsLast7DaysFromRedis() {
        String json = redisTemplate.opsForValue().get(TOP_ITEMS_LAST_7_DAYS_KEY);
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<TopItemResponseDto>>() {
            });
        } catch (Exception e) {
            log.warn("[Statistics] Redis 캐시 역직렬화 실패 key={}", TOP_ITEMS_LAST_7_DAYS_KEY, e);
            return List.of();
        }
    }

    private void cacheTopItemsLast7Days(List<TopItemResponseDto> topItems) {
        try {
            String json = objectMapper.writeValueAsString(topItems);
            redisTemplate.opsForValue().set(TOP_ITEMS_LAST_7_DAYS_KEY, json, Duration.ofDays(1));
        } catch (Exception e) {
            log.warn("[Statistics] Redis 캐시 저장 실패 key={}", TOP_ITEMS_LAST_7_DAYS_KEY, e);
        }
    }

    // 기간 별 판매량
    public List<DailySalesResponseDto> getDailySalesBetween(LocalDate from, LocalDate to) {
        return statisticsRepository.getDailySalesBetween(from, to);
    }
}
