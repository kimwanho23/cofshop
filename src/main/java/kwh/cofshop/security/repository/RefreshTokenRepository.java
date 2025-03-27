package kwh.cofshop.security.repository;

import kwh.cofshop.security.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByRefresh(String refresh);
    Optional<RefreshToken> findByMemberId(Long memberId);

    void deleteByRefresh(String refresh);
    void deleteByMemberId(Long memberId);
}
