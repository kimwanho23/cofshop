package kwh.cofshop.order.policy;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
public class BasicDeliveryPolicy implements DeliveryFeePolicy {
    @Override
    public int calculate(List<ItemOption> itemOptions) {
        return itemOptions.stream()
                .map(ItemOption::getItem)
                .distinct()
                .mapToInt(Item::getDeliveryFee)
                .sum();
    }
}