package kwh.cofshop.payment.client.portone;

public interface PortOnePaymentClient {

    PortOnePayment getPayment(String paymentId);

    PortOneCancellation cancelPayment(String paymentId, String reason);
}
