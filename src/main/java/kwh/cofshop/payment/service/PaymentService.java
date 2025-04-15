package kwh.cofshop.payment.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final IamportClient iamportClient;

    public Payment getPaymentByImpUid(String impUid){
        try {
            IamportResponse<Payment> response = iamportClient.paymentByImpUid(impUid);
            return response.getResponse();
        } catch (IamportResponseException | IOException e) {
            throw new BadRequestException(BadRequestErrorCode.BAD_REQUEST);
        }
    }
}

