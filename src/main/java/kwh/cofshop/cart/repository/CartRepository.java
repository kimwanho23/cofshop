package kwh.cofshop.cart.repository;

import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.repository.custom.CartRepositoryCustom;
import kwh.cofshop.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long>, CartRepositoryCustom {

    Optional<Cart> findByMember(Member member);
}
