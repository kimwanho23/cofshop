package kwh.cofshop.item.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.domain.QItemImg;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class ItemImgRepositoryImpl implements ItemImgRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public void deleteByItemIdAndItemImgId(Long itemId, List<Long> itemImgIds) {
        QItemImg itemImg = QItemImg.itemImg;
        if (itemImgIds == null || itemImgIds.isEmpty()) return; // 예외 처리
        queryFactory
                .delete(itemImg)
                .where(itemImg.item.id.eq(itemId),
                        (itemImg.id.in(itemImgIds)))
                .execute();
    }

    @Override
    public List<ItemImg> findByItemIdAndItemImgId(Long itemId, List<Long> itemImgIds) {
        QItemImg itemImg = QItemImg.itemImg;
        return queryFactory
                .selectFrom(itemImg)
                .where(
                        itemImg.item.id.eq(itemId),
                        itemImg.id.in(itemImgIds)
                )
                .fetch();
    }

}
