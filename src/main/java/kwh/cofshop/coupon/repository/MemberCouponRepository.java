package kwh.cofshop.coupon.repository;

import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.coupon.repository.custom.MemberCouponRepositoryCustom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long>, MemberCouponRepositoryCustom {

    @Query("""
            select new kwh.cofshop.coupon.dto.response.MemberCouponResponseDto(
                mc.id,
                mc.member.id,
                mc.coupon.id,
                mc.state,
                mc.issuedAt,
                mc.usedAt,
                mc.expiredAt
            )
            from MemberCoupon mc
            where mc.member.id = :memberId
            """)
    List<MemberCouponResponseDto> findResponseByMemberId(@Param("memberId") Long memberId);

    @EntityGraph(attributePaths = {"member", "coupon"})
    List<MemberCoupon> findByMemberId(Long memberId);

    boolean existsByMember_IdAndCoupon_Id(Long memberId, Long couponId);


}
