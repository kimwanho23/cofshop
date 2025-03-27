package kwh.cofshop.security.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "refresh_token", nullable = false)
    private String refresh; // 토큰

    @Column(name = "expiration", nullable = false)
    private LocalDateTime expiration; // 만료 시간


    @Builder
    public RefreshToken(Long memberId, String refresh, LocalDateTime expiration) {
        this.memberId = memberId;
        this.refresh = refresh;
        this.expiration = expiration;
    }

    public void update(String newRefreshToken, LocalDateTime newExpiration) {
        this.refresh = newRefreshToken;
        this.expiration = newExpiration;
    }

    public boolean isExpired() {
        return expiration.isBefore(LocalDateTime.now());
    }
}
