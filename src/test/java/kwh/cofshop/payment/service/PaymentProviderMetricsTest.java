package kwh.cofshop.payment.service;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.payment.client.portone.PortOneClientException;
import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProviderMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Mock
    private PortOnePaymentClient portOnePaymentClient;

    @InjectMocks
    private PaymentProviderService paymentProviderService;

    @BeforeEach
    void setUp() {
        Metrics.addRegistry(registry);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(registry);
    }

    @Test
    @DisplayName("PG 조회 실패: 실패 메트릭 증가")
    void lookupFailure_incrementsCounter() {
        when(portOnePaymentClient.getPayment("order-1")).thenThrow(new PortOneClientException("down"));

        assertThatThrownBy(() -> paymentProviderService.getPayment("order-1")).isInstanceOf(BusinessException.class);

        assertThat(registry.counter("payment.provider.failures", "operation", "lookup").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("PG 취소 실패: 실패 메트릭 증가")
    void cancelFailure_incrementsCounter() {
        when(portOnePaymentClient.cancelPayment(any(), any())).thenThrow(new PortOneClientException("down"));

        assertThatThrownBy(() -> paymentProviderService.cancelPayment("order-1")).isInstanceOf(BusinessException.class);

        assertThat(registry.counter("payment.provider.failures", "operation", "cancel").count()).isEqualTo(1.0);
    }
}
