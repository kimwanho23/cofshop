package kwh.cofshop.order.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderStatisticsPort {

    List<OrderDailySales> getDailySales(LocalDate date);

    List<OrderTopItemSales> getTopItemsLast7Days(LocalDateTime time);

    List<OrderDailySales> getDailySalesBetween(LocalDate start, LocalDate end);
}
