package kwh.cofshop.cart.repository;

import jakarta.persistence.LockModeType;
import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.repository.custom.CartRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long>, CartRepositoryCustom {

    Optional<Cart> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.member.id = :memberId")
    Optional<Cart> findByMemberIdWithLock(@Param("memberId") Long memberId);

    boolean existsByMemberId(Long memberId);
}
