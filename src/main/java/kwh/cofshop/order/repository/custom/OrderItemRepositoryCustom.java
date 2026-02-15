package kwh.cofshop.order.repository.custom;

import kwh.cofshop.item.domain.Item;

import java.util.List;

public interface OrderItemRepositoryCustom {

    List<Item> getPopularItems(int limit);
}
