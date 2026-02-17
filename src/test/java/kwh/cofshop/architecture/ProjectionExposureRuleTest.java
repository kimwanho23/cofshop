package kwh.cofshop.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectionExposureRuleTest {

    private static final String BASE_PACKAGE = "kwh.cofshop";
    private static final String REPOSITORY_PROJECTION_PACKAGE = ".repository.projection";

    @Test
    void controllersAndServicesMustNotExposeRepositoryProjectionAsReturnType() {
        List<String> violations = new ArrayList<>();

        for (Class<?> type : scanTypesAnnotatedWith(Controller.class, RestController.class, Service.class)) {
            for (Method method : type.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                    continue;
                }

                Type genericReturnType = method.getGenericReturnType();
                if (containsRepositoryProjection(genericReturnType)) {
                    violations.add(type.getName() + "#" + method.getName() + " -> " + genericReturnType.getTypeName());
                }
            }
        }

        assertThat(violations)
                .as("Controller/Service methods must return DTO or domain API types, not repository projections")
                .isEmpty();
    }

    @SafeVarargs
    private static List<Class<?>> scanTypesAnnotatedWith(
            Class<? extends java.lang.annotation.Annotation>... annotations) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        for (Class<? extends java.lang.annotation.Annotation> annotation : annotations) {
            scanner.addIncludeFilter(new AnnotationTypeFilter(annotation));
        }

        List<Class<?>> types = new ArrayList<>();
        scanner.findCandidateComponents(BASE_PACKAGE)
                .forEach(candidate -> {
                    try {
                        types.add(Class.forName(candidate.getBeanClassName()));
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Failed to load class: " + candidate.getBeanClassName(), e);
                    }
                });
        return types;
    }

    private static boolean containsRepositoryProjection(Type type) {
        if (type instanceof Class<?> clazz) {
            if (isRepositoryProjectionClass(clazz)) {
                return true;
            }
            if (clazz.isArray()) {
                return containsRepositoryProjection(clazz.getComponentType());
            }
            return false;
        }

        if (type instanceof ParameterizedType parameterizedType) {
            if (containsRepositoryProjection(parameterizedType.getRawType())) {
                return true;
            }
            for (Type argumentType : parameterizedType.getActualTypeArguments()) {
                if (containsRepositoryProjection(argumentType)) {
                    return true;
                }
            }
            return false;
        }

        if (type instanceof GenericArrayType arrayType) {
            return containsRepositoryProjection(arrayType.getGenericComponentType());
        }

        if (type instanceof WildcardType wildcardType) {
            for (Type upperBound : wildcardType.getUpperBounds()) {
                if (containsRepositoryProjection(upperBound)) {
                    return true;
                }
            }
            for (Type lowerBound : wildcardType.getLowerBounds()) {
                if (containsRepositoryProjection(lowerBound)) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    private static boolean isRepositoryProjectionClass(Class<?> clazz) {
        Package typePackage = clazz.getPackage();
        return typePackage != null && typePackage.getName().contains(REPOSITORY_PROJECTION_PACKAGE);
    }
}
