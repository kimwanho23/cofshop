package kwh.cofshop.member.repository.custom;

import kwh.cofshop.member.domain.Member;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepositoryCustom  {

    Optional<Member> findByMemberIdWithPessimisticLock(Long memberId);
}
