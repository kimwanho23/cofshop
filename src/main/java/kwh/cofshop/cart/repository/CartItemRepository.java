package kwh.cofshop.cart.repository;

import jakarta.persistence.LockModeType;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.repository.custom.CartItemRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long>, CartItemRepositoryCustom {
    Optional<CartItem> findByCartIdAndItemOptionId(Long cartId, Long ItemOptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.itemOption.id = :itemOptionId")
    Optional<CartItem> findByCartIdAndItemOptionIdWithLock(@Param("cartId") Long cartId,
                                                            @Param("itemOptionId") Long itemOptionId);
}
