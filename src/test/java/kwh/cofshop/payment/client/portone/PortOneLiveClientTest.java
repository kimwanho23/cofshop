package kwh.cofshop.payment.client.portone;

import kwh.cofshop.payment.properties.PortOneApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 PortOne API 호출. 없는 결제 건만 사용하므로 결제나 취소가 일어나지 않음. CI에서는 실행하지 않음
@EnabledIfEnvironmentVariable(named = "PORTONE_LIVE_SECRET", matches = ".+")
class PortOneLiveClientTest {

    private RestTemplatePortOnePaymentClient client;

    @BeforeEach
    void setUp() {
        PortOneApiProperties properties = new PortOneApiProperties();
        properties.setSecretkey(System.getenv("PORTONE_LIVE_SECRET"));
        client = new RestTemplatePortOnePaymentClient(new RestTemplate(), properties);
    }

    @Test
    @DisplayName("실제 조회: 없는 결제 건은 결제 없음으로 구분")
    void getPayment_notFound() {
        assertThatThrownBy(() -> client.getPayment("cofshop-live-" + System.currentTimeMillis()))
                .isInstanceOf(PortOnePaymentNotFoundException.class);
    }

    @Test
    @DisplayName("실제 취소: 없는 결제 건은 실패로 처리")
    void cancelPayment_notFound() {
        assertThatThrownBy(() -> client.cancelPayment("cofshop-live-" + System.currentTimeMillis(), "연결 확인"))
                .isInstanceOf(PortOneClientException.class);
    }
}
