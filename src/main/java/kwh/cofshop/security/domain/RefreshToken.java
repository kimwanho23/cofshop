package kwh.cofshop.security.domain;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.redis.core.index.Indexed;

@NoArgsConstructor
@Getter
@ToString
public class RefreshToken{

    @Id
    private Long id;

    @Indexed
    private String accessToken;

    private String refreshToken;

    public RefreshToken(Long id, String accessToken, String refreshToken){
        this.id = id;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}