package kwh.cofshop.cart.repository;

import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.repository.custom.CartItemRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long>, CartItemRepositoryCustom {

    Optional<CartItem> findByItemOptionIdAndItemId(Long optionId, Long itemId);
}
