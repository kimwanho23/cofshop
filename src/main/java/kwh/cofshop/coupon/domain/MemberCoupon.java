package kwh.cofshop.coupon.domain;

import jakarta.persistence.*;
import kwh.cofshop.member.domain.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_coupon",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "coupon_id"})
        })
public class MemberCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_coupon_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    private CouponState state;

    @Column(nullable = false)
    private LocalDate issuedAt; // 발급일자

    private LocalDate usedAt; // 사용일자

    @Builder
    public MemberCoupon(Long id, Member member, Coupon coupon,
                        CouponState state, LocalDate issuedAt, LocalDate usedAt) {
        this.id = id;
        this.member = member;
        this.coupon = coupon;
        this.state = state;
        this.issuedAt = issuedAt;
        this.usedAt = usedAt;
    }

    public static MemberCoupon createMemberCoupon(Member member, Coupon coupon) {
        return MemberCoupon.builder()
                .member(member)
                .coupon(coupon)
                .state(CouponState.AVAILABLE)
                .issuedAt(LocalDate.now()) // 발급일자
                .usedAt(null)
                .build();
    }

    public boolean isAvailable() { // 사용 가능 여부
        return this.state == CouponState.AVAILABLE;
    }

    public void useCoupon(){
        this.state = CouponState.USED;
        this.usedAt = LocalDate.now();
    }

    public void expireCoupon(){
        this.state = CouponState.EXPIRED;
    }
}
