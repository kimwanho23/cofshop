package kwh.cofshop.member.repository;

import kwh.cofshop.member.domain.MemberLoginHistory;
import kwh.cofshop.member.event.MemberLoginEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberLoginHistoryRepository extends JpaRepository<MemberLoginHistory, Long> {

    @Query("""
            select new kwh.cofshop.member.event.MemberLoginEvent(
                h.memberId,
                h.loginDt,
                h.ipAddress,
                h.device
            )
            from MemberLoginHistory h
            where h.memberId = :memberId
            order by h.loginDt desc
            """)
    List<MemberLoginEvent> findLoginEventsByMemberId(@Param("memberId") Long memberId);

    List<MemberLoginHistory> findAllByMemberIdOrderByLoginDtDesc(Long memberId);

}
