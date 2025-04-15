package kwh.cofshop.coupon.repository;

import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.repository.custom.MemberCouponRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long>, MemberCouponRepositoryCustom {

    List<MemberCoupon> findByMemberId(Long memberId);


}
