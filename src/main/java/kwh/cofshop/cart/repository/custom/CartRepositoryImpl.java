package kwh.cofshop.cart.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.domain.QCartItem;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.mapper.CartItemMapper;
import kwh.cofshop.item.domain.*;
import kwh.cofshop.item.dto.response.ItemOptionResponseDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.mapper.ItemMapper;
import kwh.cofshop.item.mapper.ItemOptionMapper;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final CartItemMapper cartItemMapper;

    @Override
    public List<CartItemResponseDto> findCartItemsByMember(Member member) { // Member Cart 조회, Item, ItemOption 등 조인
        QCartItem cartItem = QCartItem.cartItem;
        QItem item = QItem.item;
        QItemImg itemImg = QItemImg.itemImg;
        QItemOption itemOption = QItemOption.itemOption;

        List<CartItem> cartItems = queryFactory
                .selectFrom(cartItem)
                .join(cartItem.item, item).fetchJoin()
                .join(cartItem.itemOption, itemOption).fetchJoin()
                .leftJoin(item.itemImgs, itemImg).fetchJoin()
                .where(cartItem.cart.member.eq(member)
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
                .where(cartItem.cart.cartId.eq(cartId))
                .execute();
    }
}
