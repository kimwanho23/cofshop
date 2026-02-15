package kwh.cofshop.statistics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import kwh.cofshop.statistics.dto.DailySalesResponseDto;
import kwh.cofshop.statistics.dto.TopItemResponseDto;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when(statisticsRepository.getDailySales(any())).thenReturn(List.of(new DailySalesResponseDto(LocalDate.now(), 10L, 1000L)));

        List<DailySalesResponseDto> result = statisticsService.getDailySales(LocalDate.now());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("최근 7일 인기 상품")
    void getTopItemsLast7Days() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ranking:top_items:last7days")).thenReturn(null);
        when(statisticsRepository.getTopItemsLast7Days(any())).thenReturn(
                List.of(new TopItemResponseDto(1L, "커피", 10, 1000, LocalDateTime.now()))
        );

        List<TopItemResponseDto> result = statisticsService.getTopItemsLast7Days();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("최근 7일 인기 상품: 캐시 히트면 DB 조회 생략")
    void getTopItemsLast7Days_cacheHit() throws JsonProcessingException {
        TopItemResponseDto cached = new TopItemResponseDto(1L, "커피", 10, 1000, LocalDateTime.now());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ranking:top_items:last7days")).thenReturn("[{\"itemId\":1}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(cached));

        List<TopItemResponseDto> result = statisticsService.getTopItemsLast7Days();

        assertThat(result).hasSize(1);
        verify(statisticsRepository, never()).getTopItemsLast7Days(any());
    }

    @Test
    @DisplayName("최근 7일 인기 상품(레디스): 캐시 미스")
    void getTopItemsLast7DaysFromRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ranking:top_items:last7days")).thenReturn(null);

        List<TopItemResponseDto> result = statisticsService.getTopItemsLast7DaysFromRedis();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("최근 7일 인기 상품(레디스): 캐시 히트")
    void getTopItemsLast7DaysFromRedis_hit() throws JsonProcessingException {
        TopItemResponseDto topItem = new TopItemResponseDto(1L, "커피", 10, 1000, LocalDateTime.now());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ranking:top_items:last7days")).thenReturn("[{\"itemId\":1}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(topItem));

        List<TopItemResponseDto> result = statisticsService.getTopItemsLast7DaysFromRedis();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("최근 7일 인기 상품(레디스): 역직렬화 실패")
    void getTopItemsLast7DaysFromRedis_deserializeFail() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ranking:top_items:last7days")).thenReturn("invalid-json");
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenThrow(new JsonProcessingException("invalid") {
                });

        List<TopItemResponseDto> result = statisticsService.getTopItemsLast7DaysFromRedis();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("기간 매출 조회")
    void getDailySalesBetween() {
        when(statisticsRepository.getDailySalesBetween(any(), any()))
                .thenReturn(List.of(new DailySalesResponseDto(LocalDate.now(), 10L, 1000L)));

        List<DailySalesResponseDto> result = statisticsService.getDailySalesBetween(LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(result).hasSize(1);
    }
}
