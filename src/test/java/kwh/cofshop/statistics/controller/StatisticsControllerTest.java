package kwh.cofshop.statistics.controller;

import kwh.cofshop.statistics.dto.DailySalesDto;
import kwh.cofshop.statistics.dto.TopItemDto;
import kwh.cofshop.statistics.service.StatisticsService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private StatisticsController statisticsController;

    @BeforeEach
    void setUp() {
        mockMvc = StandaloneMockMvcFactory.build(statisticsController);
    }

    @Test
    @DisplayName("일일 매출 조회")
    void getDailySales() throws Exception {
        when(statisticsService.getDailySales(any()))
                .thenReturn(List.of(new DailySalesDto(LocalDate.now(), 10L, 1000L)));

        mockMvc.perform(get("/api/statistics/daily-sales")
                        .param("date", LocalDate.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("최근 7일 인기 상품 조회")
    void getTopItemsLast7Days() throws Exception {
        when(statisticsService.getTopItemsLast7Days())
                .thenReturn(List.of(new TopItemDto(1L, "커피", 10, 1000, LocalDateTime.now())));

        mockMvc.perform(get("/api/statistics/last-7days"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("기간 매출 조회")
    void getSalesBetween() throws Exception {
        when(statisticsService.getDailySalesBetween(any(), any()))
                .thenReturn(List.of(new DailySalesDto(LocalDate.now(), 10L, 1000L)));

        mockMvc.perform(get("/api/statistics/sales-between")
                        .param("start", LocalDate.now().minusDays(7).toString())
                        .param("end", LocalDate.now().toString()))
                .andExpect(status().isOk());
    }
}
