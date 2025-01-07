package kwh.cofshop.member.dto;

import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.domain.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MemberResponseDto {

    private Long memberId;  // 사용자 고유 ID
    private String email;   // 사용자 이메일
    private String memberName;  // 사용자 이름
    private String tel;  // 사용자 전화번호
    private Role role; // 사용자 정보


    private MemberState memberState;  // 회원 상태
    private Integer point;  // 사용자 포인트
    private LocalDateTime createdAt;  // 가입일자
    private LocalDateTime lastPasswordChange;  // 비밀번호 변경일자
    private LocalDateTime lastLogin;  // 마지막 로그인 시간

}
