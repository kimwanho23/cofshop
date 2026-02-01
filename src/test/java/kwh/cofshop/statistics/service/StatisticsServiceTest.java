package kwh.cofshop.statistics.service;

import kwh.cofshop.statistics.dto.DailySalesDto;
import kwh.cofshop.statistics.dto.TopItemDto;
import kwh.cofshop.statistics.repository.StatisticsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private StatisticsRepository statisticsRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    @DisplayName("일일 매출 조회")
    void getDailySales() {
        when(statisticsRepository.getDailySales(any())).thenReturn(List.of(new DailySalesDto(LocalDate.now(), 10L, 1000L)));

        List<DailySalesDto> result = statisticsService.getDailySales(LocalDate.now());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("최근 7일 인기 상품")
    void getTopItemsLast7Days() {
        when(statisticsRepository.getTopItemsLast7Days(any())).thenReturn(
                List.of(new TopItemDto(1L, "커피", 10, 1000, LocalDateTime.now()))
        );

        List<TopItemDto> result = statisticsService.getTopItemsLast7Days();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("최근 7일 인기 상품(레디스)")
    void getTopItemsLast7DaysFromRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ranking:top_items:last7days")).thenReturn(null);

        List<TopItemDto> result = statisticsService.getTopItemsLast7DaysFromRedis();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("기간 매출 조회")
    void getDailySalesBetween() {
        when(statisticsRepository.getDailySalesBetween(any(), any()))
                .thenReturn(List.of(new DailySalesDto(LocalDate.now(), 10L, 1000L)));

        List<DailySalesDto> result = statisticsService.getDailySalesBetween(LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(result).hasSize(1);
    }
}