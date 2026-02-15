package kwh.cofshop.member.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDto {
    private Long memberId;
    private String email;
    private String accessToken;
    private String refreshToken;
    private boolean passwordChangeRequired; // 비밀번호 변경 권장 여부
}