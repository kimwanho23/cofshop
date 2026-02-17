package kwh.cofshop.member.policy.impl;

import kwh.cofshop.member.policy.MembershipPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("PURPLE")
public class PurpleMembershipPolicy implements MembershipPolicy {
    private static final double DISCOUNT_RATE = 0.10; // 할인율 10%

    @Override
    public int calculateDiscount(int price) {
        return (int) (price - (price * DISCOUNT_RATE));
    }
}
