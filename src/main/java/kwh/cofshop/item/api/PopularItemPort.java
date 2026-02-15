package kwh.cofshop.item.api;

import kwh.cofshop.item.domain.Item;

import java.util.List;

public interface PopularItemPort {

    List<Item> getPopularItems(int limit);
}
