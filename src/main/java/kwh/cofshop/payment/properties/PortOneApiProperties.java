package kwh.cofshop.payment.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "imp.api")
public class PortOneApiProperties {

    @NotBlank
    private String secretkey;

    @NotBlank
    private String baseUrl = "https://api.portone.io";
}
