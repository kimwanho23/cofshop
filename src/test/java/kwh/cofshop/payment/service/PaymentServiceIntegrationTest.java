package kwh.cofshop.payment.service;

import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentServiceIntegrationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Mock
    private PortOnePaymentClient portOnePaymentClient;

    @Mock
    private PaymentRefundTxService paymentRefundTxService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("서비스 생성")
    void createService() {
        assertThat(paymentService).isNotNull();
    }
}
