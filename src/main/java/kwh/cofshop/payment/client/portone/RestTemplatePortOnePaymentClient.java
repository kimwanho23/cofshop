package kwh.cofshop.payment.client.portone;

import kwh.cofshop.payment.properties.PortOneApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RestTemplatePortOnePaymentClient implements PortOnePaymentClient {

    private final RestTemplate portOneRestTemplate;
    private final PortOneApiProperties portOneApiProperties;

    @Override
    public PortOnePayment getPayment(String paymentId) {
        try {
            ResponseEntity<PortOnePayment> response = portOneRestTemplate.exchange(
                    portOneApiProperties.getBaseUrl() + "/payments/{paymentId}",
                    HttpMethod.GET,
                    new HttpEntity<>(authorizedHeaders()),
                    PortOnePayment.class,
                    paymentId
            );

            if (response.getBody() == null) {
                throw new PortOneClientException("PortOne payment response is empty");
            }

            return response.getBody();
        } catch (RestClientException e) {
            throw new PortOneClientException("Failed to fetch payment from PortOne", e);
        }
    }

    @Override
    public PortOneCancellation cancelPayment(String paymentId, String reason) {
        try {
            HttpHeaders headers = authorizedHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(
                    Map.of("reason", reason),
                    headers
            );

            ResponseEntity<PortOneCancellation> response = portOneRestTemplate.exchange(
                    portOneApiProperties.getBaseUrl() + "/payments/{paymentId}/cancel",
                    HttpMethod.POST,
                    request,
                    PortOneCancellation.class,
                    paymentId
            );

            if (response.getBody() == null) {
                throw new PortOneClientException("PortOne cancel response is empty");
            }

            return response.getBody();
        } catch (RestClientException e) {
            throw new PortOneClientException("Failed to cancel payment in PortOne", e);
        }
    }

    private HttpHeaders authorizedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "PortOne " + portOneApiProperties.getSecretkey());
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        return headers;
    }
}
