package kwh.cofshop.member.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class MemberLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_login_history_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @CreatedDate
    private LocalDateTime loginDt; // 로그인 날짜

    private String ipAddress; // 로그인 IP

    private String device; // 로그인 디바이스

    @Builder
    public MemberLoginHistory(Long id, String email, LocalDateTime loginDt, String ipAddress, String device) {
        this.id = id;
        this.email = email;
        this.loginDt = loginDt;
        this.ipAddress = ipAddress;
        this.device = device;
    }
}
