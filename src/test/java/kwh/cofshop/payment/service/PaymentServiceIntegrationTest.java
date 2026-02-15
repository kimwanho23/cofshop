package kwh.cofshop.payment.service;

import kwh.cofshop.order.api.OrderPaymentPreparePort;
import kwh.cofshop.order.api.OrderStatePort;
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
    private OrderPaymentPreparePort orderPaymentPreparePort;

    @Mock
    private OrderStatePort orderStatePort;

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Mock
    private PaymentProviderService paymentProviderService;

    @Mock
    private PaymentRefundTxService paymentRefundTxService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("service creation")
    void createService() {
        assertThat(paymentService).isNotNull();
    }
}
