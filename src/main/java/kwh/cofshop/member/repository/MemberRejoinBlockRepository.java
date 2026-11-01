package kwh.cofshop.member.repository;

import kwh.cofshop.member.domain.MemberRejoinBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface MemberRejoinBlockRepository extends JpaRepository<MemberRejoinBlock, Long> {

    Optional<MemberRejoinBlock> findByIdentifierHash(String identifierHash);

    boolean existsByIdentifierHashInAndExpiresAtAfter(Collection<String> identifierHashes, LocalDateTime now);

    @Modifying
    @Query("delete from MemberRejoinBlock b where b.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
