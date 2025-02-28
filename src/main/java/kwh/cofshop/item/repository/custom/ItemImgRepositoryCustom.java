package kwh.cofshop.item.repository.custom;

import java.util.List;

public interface ItemImgRepositoryCustom {

    void deleteByItemIdAndItemImgId(Long itemId, List<Long> itemImgIds);
}
