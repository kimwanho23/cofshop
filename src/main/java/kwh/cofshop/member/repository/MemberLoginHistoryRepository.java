package kwh.cofshop.member.repository;

import kwh.cofshop.member.domain.MemberLoginHistory;
import kwh.cofshop.member.event.MemberLoginEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    List<MemberLoginEvent> findLoginEventsByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    @Query("select h.id from MemberLoginHistory h where h.loginDt < :cutoff")
    List<Long> findIdsLoggedInBefore(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

}
