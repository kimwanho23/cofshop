package kwh.cofshop.item.repository.custom;

import kwh.cofshop.item.domain.ItemImg;

import java.util.List;

public interface ItemImgRepositoryCustom {

    void deleteByItemIdAndItemImgId(Long itemId, List<Long> itemImgIds);

    List<ItemImg> findByItemIdAndItemImgId(Long itemId, List<Long> itemImgIds);
}
