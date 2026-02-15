package kwh.cofshop.coupon.infrastructure.persistence.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.domain.QCoupon;
import kwh.cofshop.coupon.domain.QMemberCoupon;
import kwh.cofshop.member.domain.QMember;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class MemberCouponRepositoryImpl implements MemberCouponRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<MemberCoupon> findValidCouponByMemberCouponId(Long memberCouponId, Long memberId, LocalDate today) {
        QMemberCoupon memberCoupon = QMemberCoupon.memberCoupon;
        QCoupon coupon = QCoupon.coupon;
        QMember member = QMember.member;

        return Optional.ofNullable(
                queryFactory.selectFrom(memberCoupon)
                        .join(memberCoupon.coupon, coupon).fetchJoin()
                        .join(memberCoupon.member, member)
                        .where(
                                memberCoupon.id.eq(memberCouponId),
                                memberCoupon.member.id.eq(memberId),
                                memberCoupon.state.eq(CouponState.AVAILABLE),
                                coupon.state.eq(CouponState.AVAILABLE),
                                coupon.validFrom.loe(today),
                                coupon.validTo.goe(today)
                        )
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)

                        .fetchOne()
        );
    }

    @Override
    public List<MemberCoupon> findByCouponExpired(CouponState state, LocalDate date) {
        QMemberCoupon memberCoupon = QMemberCoupon.memberCoupon;
        QCoupon coupon = QCoupon.coupon;

        return queryFactory
                .selectFrom(memberCoupon)
                .join(memberCoupon.coupon, coupon).fetchJoin()
                .where(
                        memberCoupon.state.eq(state),
                        coupon.validTo.lt(date)
                )
                .fetch();
    }
}
