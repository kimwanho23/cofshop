package kwh.cofshop.support;

import kwh.cofshop.global.exception.GlobalExceptionHandler;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

public final class StandaloneMockMvcFactory {

    private StandaloneMockMvcFactory() {
    }

    public static MockMvc build(Object controller, HandlerMethodArgumentResolver... resolvers) {
        return build(new Object[]{controller}, resolvers);
    }

    public static MockMvc build(Object[] controllers, HandlerMethodArgumentResolver... resolvers) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        HandlerMethodArgumentResolver[] allResolvers = combineResolvers(
                new PageableHandlerMethodArgumentResolver(),
                resolvers
        );

        return MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(allResolvers)
                .setValidator(validator)
                .build();
    }

    private static HandlerMethodArgumentResolver[] combineResolvers(
            HandlerMethodArgumentResolver first,
            HandlerMethodArgumentResolver[] rest
    ) {
        HandlerMethodArgumentResolver[] combined = new HandlerMethodArgumentResolver[rest.length + 1];
        combined[0] = first;
        System.arraycopy(rest, 0, combined, 1, rest.length);
        return combined;
    }
}
