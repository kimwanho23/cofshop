package kwh.cofshop.config.argumentResolver;


import java.lang.annotation.*;

@Target(ElementType.PARAMETER) // 메서드 매개변수에 사용 가능
@Retention(RetentionPolicy.RUNTIME) // 런타임에도 유지되어야 함
public @interface LoginMember {
}