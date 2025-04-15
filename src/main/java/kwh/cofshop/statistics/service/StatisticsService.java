package kwh.cofshop.statistics.service;

import kwh.cofshop.statistics.dto.DailySalesDto;
import kwh.cofshop.statistics.dto.TopItemDto;
import kwh.cofshop.statistics.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsRepository statisticsRepository;

    // 일별 판매량
    public List<DailySalesDto> getDailySales(LocalDate localDate){
        return statisticsRepository.getDailySales(localDate);
    }

    // 최근 7일간 인기 상품
    public List<TopItemDto> getTopItemsLast7Days(){
        LocalDateTime current7days = LocalDateTime.now().minusDays(7);
        return statisticsRepository.getTopItemsLast7Days(current7days);
    }

    // 기간 별 판매량
    public List<DailySalesDto> getDailySalesBetween(LocalDate from, LocalDate to) {
        return statisticsRepository.getDailySalesBetween(from, to);
    }


}
