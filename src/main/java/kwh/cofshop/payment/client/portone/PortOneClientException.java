package kwh.cofshop.payment.client.portone;

public class PortOneClientException extends RuntimeException {

    public PortOneClientException(String message) {
        super(message);
    }

    public PortOneClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
