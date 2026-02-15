package kwh.cofshop.member.api;

import kwh.cofshop.member.domain.Member;

import java.util.Optional;

public interface MemberReadPort {

    Member getById(Long memberId);

    Member getByIdWithLock(Long memberId);

    Optional<Member> findByEmail(String email);
}
