package kwh.cofshop.order.repository.custom;

import java.util.List;

public interface OrderItemRepositoryCustom {

    List<Long> getPopularItemIds(int limit);
}
