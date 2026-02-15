package kwh.cofshop.coupon.infrastructure.persistence.repository;

import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.infrastructure.persistence.repository.custom.MemberCouponRepositoryCustom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long>, MemberCouponRepositoryCustom {

    @EntityGraph(attributePaths = {"member", "coupon"})
    List<MemberCoupon> findByMemberId(Long memberId);

    boolean existsByMember_IdAndCoupon_Id(Long memberId, Long couponId);


}
