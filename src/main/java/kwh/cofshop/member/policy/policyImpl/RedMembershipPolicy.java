package kwh.cofshop.member.policy.policyImpl;

import kwh.cofshop.member.policy.MembershipPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("RED")
public class RedMembershipPolicy implements MembershipPolicy { // 할인율 5%
    private static final double DISCOUNT_RATE = 0.05; // 할인율 10%

    @Override
    public int calculateDiscount(int price) {
        return (int) (price - (price * DISCOUNT_RATE));
    }
}
