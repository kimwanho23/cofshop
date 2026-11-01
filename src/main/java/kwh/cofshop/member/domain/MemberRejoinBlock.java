package kwh.cofshop.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 탈퇴 회원의 이메일·전화번호 해시. 재가입 제한에만 쓰고 기간이 지나면 삭제한다
@Entity
@Table(name = "member_rejoin_block")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberRejoinBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_rejoin_block_id")
    private Long id;

    @Column(name = "identifier_hash", nullable = false, unique = true, length = 64)
    private String identifierHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public MemberRejoinBlock(String identifierHash, LocalDateTime expiresAt) {
        this.identifierHash = identifierHash;
        this.expiresAt = expiresAt;
    }

    public void extendUntil(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
