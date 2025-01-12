package kwh.cofshop.item.repository.custom;

import kwh.cofshop.item.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItemRepositoryCustom {

    Page<Item> findByItemName(String itemName, Pageable pageable);
}
