package kwh.cofshop.payment.client.portone;

import kwh.cofshop.payment.properties.PortOneApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class RestTemplatePortOnePaymentClientTest {

    private MockRestServiceServer server;
    private RestTemplatePortOnePaymentClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        PortOneApiProperties properties = new PortOneApiProperties();
        properties.setSecretkey("test-only");
        properties.setBaseUrl("https://portone.test");
        client = new RestTemplatePortOnePaymentClient(restTemplate, properties);
    }

    @Test
    @DisplayName("결제 조회: 404는 결제 없음으로 구분")
    void getPayment_notFound() {
        server.expect(requestTo("https://portone.test/payments/order-1")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getPayment("order-1")).isInstanceOf(PortOnePaymentNotFoundException.class);
    }

    @Test
    @DisplayName("결제 조회: 서버 오류는 결제 없음으로 처리하지 않음")
    void getPayment_serverError() {
        server.expect(requestTo("https://portone.test/payments/order-1"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.getPayment("order-1"))
                .isInstanceOf(PortOneClientException.class)
                .satisfies(e -> assertThat(e).isNotInstanceOf(PortOnePaymentNotFoundException.class));
    }
}
