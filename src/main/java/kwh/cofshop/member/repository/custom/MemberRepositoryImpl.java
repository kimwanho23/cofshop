package kwh.cofshop.member.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.QMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class MemberRepositoryImpl implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;

        @Override
        public Optional<Member> findByMemberIdWithPessimisticLock(Long memberId) {
            QMember member = QMember.member;
            return Optional.ofNullable(
                    queryFactory.selectFrom(member)
                            .where(member.id.eq(memberId))
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE) // 비관적 락
                            .fetchOne()
            );
        }
}
