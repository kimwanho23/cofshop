package kwh.cofshop.cart.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.domain.QCartItem;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.item.domain.QItem;
import kwh.cofshop.item.domain.QItemOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CartItemRepositoryImpl implements CartItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CartItemResponseDto> findCartItemsByMemberId(Long memberId) {
        QCartItem cartItem = QCartItem.cartItem;

        return queryFactory
                .select(Projections.fields(
                        CartItemResponseDto.class,
                        cartItem.cart.id.as("cartId"),
                        cartItem.quantity,
                        cartItem.item.id.as("itemId"),
                        cartItem.itemOption.id.as("optionId")
                ))
                .from(cartItem)
                .where(cartItem.cart.member.id.eq(memberId))
                .fetch();
    }

    @Override
    public Long sumTotalPriceByMemberId(Long memberId) {
        QCartItem cartItem = QCartItem.cartItem;
        QItem item = QItem.item;
        QItemOption itemOption = QItemOption.itemOption;

        // 주문 금액과 같은 계산을 쓰도록 도메인 가격을 사용 (SQL 나눗셈은 절사 방식과 반환 타입이 다름)
        return queryFactory
                .selectFrom(cartItem)
                .join(cartItem.itemOption, itemOption).fetchJoin()
                .join(itemOption.item, item).fetchJoin()
                .where(cartItem.cart.member.id.eq(memberId))
                .fetch()
                .stream()
                .mapToLong(line -> (long) line.getItemOption().getTotalPrice() * line.getQuantity())
                .sum();
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
