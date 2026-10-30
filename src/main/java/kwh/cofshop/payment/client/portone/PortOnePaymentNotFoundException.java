package kwh.cofshop.payment.client.portone;

// 결제창을 열지 않은 주문처럼 PortOne에 결제 건이 없는 경우. 조회 실패와 구분해야 함
public class PortOnePaymentNotFoundException extends PortOneClientException {

    public PortOnePaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
