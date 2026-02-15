@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
        allowedDependencies = {"order::api", "global::annotation", "global::exception", "global::errorcodes"}
)
package kwh.cofshop.payment;
