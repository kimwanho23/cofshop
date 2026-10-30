package kwh.cofshop.payment.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SupportedPaymentMethodValidator.class)
public @interface SupportedPaymentMethod {

    String message() default "지원하지 않는 결제수단입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
