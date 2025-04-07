package kwh.cofshop.cart.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.domain.QCartItem;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.mapper.CartItemMapper;
import kwh.cofshop.item.domain.ImgType;
import kwh.cofshop.item.domain.QItem;
import kwh.cofshop.item.domain.QItemImg;
import kwh.cofshop.item.domain.QItemOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CartItemRepositoryImpl implements CartItemRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    private final CartItemMapper cartItemMapper;

    @Override
    public List<CartItemResponseDto> findCartItemsByMemberId(Long memberId) { // Member Cart 조회, Item, ItemOption 등 조인
        QCartItem cartItem = QCartItem.cartItem;
        QItem item = QItem.item;
        QItemImg itemImg = QItemImg.itemImg;
        QItemOption itemOption = QItemOption.itemOption;

        List<CartItem> cartItems = queryFactory
                .selectFrom(cartItem)
                .join(cartItem.item, item).fetchJoin()
                .join(cartItem.itemOption, itemOption).fetchJoin()
                .leftJoin(item.itemImgs, itemImg).fetchJoin()
                .where(cartItem.cart.member.id.eq(memberId)
                        .and(itemImg.imgType.eq(ImgType.REPRESENTATIVE)))
                .fetch();

        return cartItems.stream()
                .map(cartItemMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override // 해당 Cart 아이템 전체 삭제
    public void deleteAllByCartId(Long cartId) {
        QCartItem cartItem = QCartItem.cartItem;
        queryFactory
                .delete(cartItem)
                .where(cartItem.cart.id.eq(cartId))
                .execute();
    }

    @Override
    public Optional<CartItem> findByItemAndOptionAndCart(Long itemId, Long optionId, Long cartId) {
        QCartItem cartItem = QCartItem.cartItem;

        return Optional.ofNullable(
                queryFactory.selectFrom(cartItem)
                        .where(
                                cartItem.item.id.eq(itemId),
                                cartItem.itemOption.id.eq(optionId),
                                cartItem.cart.id.eq(cartId)
                        )
                        .fetchOne()
        );
    }
}
