package kwh.cofshop.member.repository;

import kwh.cofshop.member.domain.MemberLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberLoginHistoryRepository extends JpaRepository<MemberLoginHistory, Long> {

}
