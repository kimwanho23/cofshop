package kwh.cofshop.payment.client.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneCancellation(
        String id,
        String status
) {
}
