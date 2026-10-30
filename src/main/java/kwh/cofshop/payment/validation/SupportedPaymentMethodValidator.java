package kwh.cofshop.payment.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import kwh.cofshop.payment.domain.PaymentMethod;

public class SupportedPaymentMethodValidator implements ConstraintValidator<SupportedPaymentMethod, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return PaymentMethod.isSupported(value);
    }
}
