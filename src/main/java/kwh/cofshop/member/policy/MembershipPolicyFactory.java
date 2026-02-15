package kwh.cofshop.member.policy;

import kwh.cofshop.member.domain.MembershipTier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MembershipPolicyFactory {
    private final Map<String, MembershipPolicy> policyMap;

    public MembershipPolicyFactory(@Qualifier Map<String, MembershipPolicy> policyMap) {
        this.policyMap = policyMap;
    }

    public MembershipPolicy getPolicy(MembershipTier tier) {
        return policyMap.getOrDefault(tier.name(), policyMap.get("BASIC")); // 기본값 basic
    }
}
