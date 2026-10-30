package kwh.cofshop.payment.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import kwh.cofshop.payment.domain.PGProvider;

public class SupportedPgProviderValidator implements ConstraintValidator<SupportedPgProvider, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return PGProvider.isSupported(value);
    }
}
