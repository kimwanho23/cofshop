package kwh.cofshop.order.policy;

import kwh.cofshop.item.domain.ItemOption;

import java.util.List;

public interface DeliveryFeePolicy {
    int calculate(List<ItemOption> itemOptions);
}