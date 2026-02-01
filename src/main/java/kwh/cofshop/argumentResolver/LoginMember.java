package kwh.cofshop.argumentResolver;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER) // 메서드 매개변수에 사용 가능
@Retention(RetentionPolicy.RUNTIME) // 런타임에도 유지되어야 함
public @interface LoginMember {
}