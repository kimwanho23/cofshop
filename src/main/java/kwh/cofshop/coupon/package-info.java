@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
        allowedDependencies = {
                "member::domain",
                "member::api",
                "global::annotation",
                "global::domain",
                "global::exception",
                "global::errorcodes"
        }
)
package kwh.cofshop.coupon;
