@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
        allowedDependencies = {
                "member::domain",
                "member::api",
                "member::event",
                "global::exception",
                "global::errorcodes"
        }
)
package kwh.cofshop.security;
