package kwh.cofshop.member.repository.custom;

import kwh.cofshop.member.domain.Member;

import java.util.Optional;

public interface MemberRepositoryCustom  {
    Optional<Member> findByMemberIdWithPessimisticLock(Long memberId);
}
