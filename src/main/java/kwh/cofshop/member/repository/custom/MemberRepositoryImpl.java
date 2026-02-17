package kwh.cofshop.member.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.member.domain.QMember;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<MemberResponseDto> findMemberResponseById(Long memberId) {
        QMember member = QMember.member;

        return Optional.ofNullable(
                queryFactory
                        .select(Projections.fields(
                                MemberResponseDto.class,
                                member.id.as("memberId"),
                                member.email,
                                member.memberName,
                                member.tel,
                                member.role,
                                member.memberState,
                                member.point,
                                member.createdAt,
                                member.lastPasswordChange,
                                member.lastLogin
                        ))
                        .from(member)
                        .where(member.id.eq(memberId))
                        .fetchOne()
        );
    }

    @Override
    public List<MemberResponseDto> findAllMemberResponses() {
        QMember member = QMember.member;

        return queryFactory
                .select(Projections.fields(
                        MemberResponseDto.class,
                        member.id.as("memberId"),
                        member.email,
                        member.memberName,
                        member.tel,
                        member.role,
                        member.memberState,
                        member.point,
                        member.createdAt,
                        member.lastPasswordChange,
                        member.lastLogin
                ))
                .from(member)
                .fetch();
    }
}
