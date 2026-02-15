package kwh.cofshop.member.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class MemberLoginEvent {
    private Long memberId;
    private LocalDateTime loginDt; // 로그인 날짜
    private String ipAddress;
    private String device; // 로그인 디바이스

    @Builder
    public MemberLoginEvent(Long memberId, LocalDateTime loginDt, String ipAddress, String device) {
        this.memberId = memberId;
        this.loginDt = loginDt;
        this.ipAddress = ipAddress;
        this.device = device;
    }
}
