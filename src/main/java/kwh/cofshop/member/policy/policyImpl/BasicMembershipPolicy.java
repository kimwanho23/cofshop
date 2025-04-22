package kwh.cofshop.member.policy.policyImpl;

import kwh.cofshop.member.policy.MembershipPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("BASIC")
public class BasicMembershipPolicy implements MembershipPolicy {
    @Override
    public int calculateDiscount(int price) {
        return price;
    }
}
