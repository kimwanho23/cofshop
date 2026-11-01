package kwh.cofshop.payment.client.portone;

import kwh.cofshop.payment.properties.PortOneApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
    @DisplayName("결제 조회: 공식 스키마의 결제 완료 응답을 읽음")
    void getPayment_paidResponse() {
        server.expect(requestTo("https://portone.test/payments/order-1"))
                .andExpect(header("Authorization", "PortOne test-only"))
                .andRespond(withSuccess("""
                        {"status":"PAID","id":"order-1","transactionId":"tx-1","pgTxId":"pg-1",
                         "amount":{"total":1000,"taxFree":0,"discount":0,"paid":1000,"cancelled":0,"cancelledTaxFree":0},
                         "currency":"KRW","merchantId":"m","storeId":"s"}
                        """, MediaType.APPLICATION_JSON));

        PortOnePayment payment = client.getPayment("order-1");

        assertThat(payment.status()).isEqualTo("PAID");
        assertThat(payment.transactionId()).isEqualTo("tx-1");
        assertThat(payment.pgTxId()).isEqualTo("pg-1");
        assertThat(payment.paidAmount()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("결제 취소: cancellation 안의 상태를 읽음")
    void cancelPayment_readsNestedCancellation() {
        server.expect(requestTo("https://portone.test/payments/order-1/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "PortOne test-only"))
                .andExpect(jsonPath("$.reason").value("환불"))
                .andRespond(withSuccess("""
                        {"cancellation":{"status":"FAILED","id":"cancel-1","totalAmount":1000,"reason":"환불"}}
                        """, MediaType.APPLICATION_JSON));

        PortOneCancellation cancellation = client.cancelPayment("order-1", "환불");

        assertThat(cancellation.status()).isEqualTo("FAILED");
        assertThat(cancellation.id()).isEqualTo("cancel-1");
    }

    @Test
    @DisplayName("결제 취소: cancellation이 없으면 실패로 처리")
    void cancelPayment_missingCancellation() {
        server.expect(requestTo("https://portone.test/payments/order-1/cancel"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.cancelPayment("order-1", "환불")).isInstanceOf(PortOneClientException.class);
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
