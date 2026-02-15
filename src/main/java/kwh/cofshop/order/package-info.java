@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
        allowedDependencies = {
                "member::domain",
                "member::api",
                "item::domain",
                "item::api",
                "coupon::api",
                "global::annotation",
                "global::domain",
                "global::exception",
                "global::errorcodes"
        }
)
package kwh.cofshop.order;
