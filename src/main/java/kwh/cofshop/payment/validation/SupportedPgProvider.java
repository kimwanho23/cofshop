package kwh.cofshop.payment.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SupportedPgProviderValidator.class)
public @interface SupportedPgProvider {

    String message() default "지원하지 않는 PG사입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
