package kwh.cofshop.member.dto;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MemberLoginHistoryDto {

    private Integer id;
    private String email;
    private LocalDateTime loginDt; // 로그인 날짜
    private String ipAddress;
    private String memberAgent;
}
