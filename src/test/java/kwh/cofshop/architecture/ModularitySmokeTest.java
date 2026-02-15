package kwh.cofshop.architecture;

import kwh.cofshop.CofshopApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.DependencyType;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ModularitySmokeTest {

    private static final Set<String> EXPECTED_MODULES = Set.of(
            "cart",
            "chat",
            "coupon",
            "file",
            "global",
            "item",
            "member",
            "order",
            "payment",
            "security",
            "statistics"
    );

    @Test
    void detectsApplicationModules() {
        ApplicationModules modules = modules();
        Set<String> moduleNames = modules.stream()
                .map(ApplicationModule::getName)
                .map(ModularitySmokeTest::normalizeModuleName)
                .collect(Collectors.toSet());

        assertThat(modules).isNotNull();
        assertThat(moduleNames).containsAll(EXPECTED_MODULES);
    }

    @Test
    void verifiesCriticalModulesAreClosed() {
        ApplicationModules modules = modules();

        EXPECTED_MODULES.forEach(name -> assertThat(requiredModule(modules, name).isOpen())
                .as("%s module should be closed", name)
                .isFalse());
    }

    @Test
    void verifiesKeyNamedInterfaces() {
        ApplicationModules modules = modules();

        assertThat(requiredModule(modules, "member").getNamedInterfaces().getByName("api")).isPresent();
        assertThat(requiredModule(modules, "item").getNamedInterfaces().getByName("api")).isPresent();
        assertThat(requiredModule(modules, "order").getNamedInterfaces().getByName("api")).isPresent();
        assertThat(requiredModule(modules, "coupon").getNamedInterfaces().getByName("api")).isPresent();
        assertThat(requiredModule(modules, "file").getNamedInterfaces().getByName("api")).isPresent();
        assertThat(requiredModule(modules, "global").getNamedInterfaces().getByName("annotation")).isPresent();
        assertThat(requiredModule(modules, "global").getNamedInterfaces().getByName("domain")).isPresent();
        assertThat(requiredModule(modules, "global").getNamedInterfaces().getByName("exception")).isPresent();
        assertThat(requiredModule(modules, "global").getNamedInterfaces().getByName("errorcodes")).isPresent();
    }

    @Test
    void verifiesKeyModuleDependencies() {
        ApplicationModules modules = modules();

        Set<String> paymentDeps = directDependencyNames(requiredModule(modules, "payment"), modules);
        assertThat(paymentDeps).contains("order", "global");
        assertThat(paymentDeps).doesNotContain("member", "item", "coupon", "security");

        Set<String> statisticsDeps = directDependencyNames(requiredModule(modules, "statistics"), modules);
        assertThat(statisticsDeps).containsExactly("order");

        Set<String> securityDeps = directDependencyNames(requiredModule(modules, "security"), modules);
        assertThat(securityDeps).contains("member", "global");
        assertThat(securityDeps).doesNotContain("order", "payment", "item", "coupon");
    }

    @Test
    void verifiesModuleDependencies() {
        modules().verify();
    }

    private static ApplicationModules modules() {
        return ApplicationModules.of(CofshopApplication.class);
    }

    private static ApplicationModule requiredModule(ApplicationModules modules, String name) {
        return modules.getModuleByName(name)
                .or(() -> modules.getModuleByName("kwh.cofshop." + name))
                .orElseThrow(() -> new IllegalStateException("Missing module: " + name));
    }

    private static Set<String> directDependencyNames(ApplicationModule module, ApplicationModules modules) {
        return module.getDirectDependencies(modules, DependencyType.values())
                .uniqueModules()
                .map(ApplicationModule::getName)
                .map(ModularitySmokeTest::normalizeModuleName)
                .collect(Collectors.toSet());
    }

    private static String normalizeModuleName(String moduleName) {
        int separator = moduleName.lastIndexOf('.');
        return separator >= 0 ? moduleName.substring(separator + 1) : moduleName;
    }
}
