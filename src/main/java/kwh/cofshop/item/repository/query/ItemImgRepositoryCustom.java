package kwh.cofshop.item.repository.query;

import kwh.cofshop.item.domain.ItemImg;

import java.util.List;

public interface ItemImgRepositoryCustom {

    List<ItemImg> findByItemIdAndItemImgId(Long itemId, List<Long> itemImgIds);
}
