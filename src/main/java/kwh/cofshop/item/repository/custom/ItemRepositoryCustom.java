package kwh.cofshop.item.repository.custom;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepositoryCustom {

    Page<Item> findByItemName(String itemName, Pageable pageable);
    Page<Item> searchItems(ItemSearchRequestDto requestDto, Pageable pageable);

}
