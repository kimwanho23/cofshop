package kwh.cofshop.item.repository.query;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ItemRepositoryCustom {

    Page<Item> searchItems(ItemSearchRequestDto requestDto, Pageable pageable);

}
