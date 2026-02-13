package kwh.cofshop.payment.client.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePayment(
        String id,
        String transactionId,
        String pgTxId,
        String status,
        Amount amount
) {

    public Long paidAmount() {
        if (amount == null) {
            return null;
        }
        return amount.paid();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(
            Long total,
            Long paid
    ) {
    }
}
