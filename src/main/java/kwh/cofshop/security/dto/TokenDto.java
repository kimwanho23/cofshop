package kwh.cofshop.security.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class TokenDto {

    private final String grantType;
    private final String accessToken;
    private final String refreshToken;

    @Builder
    public TokenDto(String grantType, String accessToken, String refreshToken) {
        this.grantType = grantType;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }


}