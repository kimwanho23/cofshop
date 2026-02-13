package kwh.cofshop.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "portone.browser")
public class PortOneBrowserProperties {

    private String storeId = "";

    private String channelKey = "";
}
