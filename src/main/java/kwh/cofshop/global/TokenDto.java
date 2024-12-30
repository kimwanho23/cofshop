package kwh.cofshop.global;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenDto {

    private String grantType;
    private String accessToken;
    private String refreshToken;
}