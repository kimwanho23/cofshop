package kwh.cofshop.global;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TokenDto {

    private String grantType;
    private String accessToken;
    private String refreshToken;

    @Builder
    public TokenDto(String grantType, String accessToken, String refreshToken) {
        this.grantType = grantType;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}