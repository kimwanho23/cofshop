package kwh.cofshop.cart.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
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

        NumberExpression<Long> itemDiscountRate = item.discount.coalesce(0).longValue();
        NumberExpression<Long> optionAdditionalPrice = itemOption.additionalPrice.coalesce(0).longValue();
        NumberExpression<Long> optionDiscountRate = itemOption.discountRate.coalesce(0).longValue();

        NumberExpression<Long> discountedItemPrice = item.price.longValue()
                .multiply(Expressions.numberTemplate(Long.class, "(100 - {0})", itemDiscountRate))
                .divide(100);
        NumberExpression<Long> basePrice = discountedItemPrice.add(optionAdditionalPrice);
        NumberExpression<Long> discountedOptionPrice = basePrice
                .multiply(Expressions.numberTemplate(Long.class, "(100 - {0})", optionDiscountRate))
                .divide(100);
        NumberExpression<Long> lineTotal = discountedOptionPrice.multiply(cartItem.quantity.longValue());

        return queryFactory
                .select(lineTotal.sum())
                .from(cartItem)
                .join(cartItem.item, item)
                .join(cartItem.itemOption, itemOption)
                .where(cartItem.cart.member.id.eq(memberId))
                .fetchOne();
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
