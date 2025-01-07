package kwh.cofshop.cart.repository;

import kwh.cofshop.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {


}
