package kwh.cofshop.item.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
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

    @Override
    public List<ItemImg> findByItemIdWithLock(Long itemId) {
        QItemImg itemImg = QItemImg.itemImg;
        return queryFactory
                .selectFrom(itemImg)
                .where(itemImg.item.id.eq(itemId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }

}
