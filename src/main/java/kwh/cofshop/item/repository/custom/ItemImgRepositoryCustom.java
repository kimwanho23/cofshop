package kwh.cofshop.item.repository.custom;

import kwh.cofshop.item.domain.ItemImg;

import java.util.List;

public interface ItemImgRepositoryCustom {

    List<ItemImg> findByItemIdAndItemImgId(Long itemId, List<Long> itemImgIds);


    List<ItemImg> findByItemIdWithLock(Long id);
}
