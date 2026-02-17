package kwh.cofshop.item.api;

import java.util.List;

public interface PopularItemPort {

    List<Long> getPopularItemIds(int limit);
}
