package kwh.cofshop.statistics.repository;

import kwh.cofshop.order.api.OrderTopItemSales;
import kwh.cofshop.order.api.OrderStatisticsPort;
import kwh.cofshop.statistics.dto.TopItemResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsRepositoryImplTest {

    @Mock
    private OrderStatisticsPort orderStatisticsPort;

    @InjectMocks
    private StatisticsRepositoryImpl statisticsRepository;

    @Test
    @DisplayName("최근 7일 인기 상품 매핑: large long 값 보존")
    void getTopItemsLast7Days_preserveLargeLongValues() {
        OrderTopItemSales source = new OrderTopItemSales(
                1L,
                "커피",
                3_000_000_000L,
                9_000_000_000L,
                LocalDateTime.now()
        );
        when(orderStatisticsPort.getTopItemsLast7Days(any())).thenReturn(List.of(source));

        List<TopItemResponseDto> result = statisticsRepository.getTopItemsLast7Days(LocalDateTime.now().minusDays(7));

        assertThat(result).hasSize(1);
        TopItemResponseDto dto = result.get(0);
        assertThat(dto.getItemId()).isEqualTo(1L);
        assertThat(dto.getTotalSold()).isEqualTo(3_000_000_000L);
        assertThat(dto.getTotalRevenue()).isEqualTo(9_000_000_000L);
    }
}
