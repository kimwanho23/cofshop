package kwh.cofshop.cart.repository;

import kwh.cofshop.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByOptionIdAndItemId(Long optionId, Long itemId);
}
